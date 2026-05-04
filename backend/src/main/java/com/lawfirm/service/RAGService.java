package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.RAGChatRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.entity.KnowledgeArticle;
import com.lawfirm.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG服务（整合知识库检索+LLM）
 * 简化实现版本，核心功能优先
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final LLMApiService llmApiService;
    private final AIConfigService aiConfigService;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final EmbeddingService embeddingService;
    private final QdrantVectorService qdrantVectorService;
    private final AILogService aiLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * RAG检索问答
     *
     * @param request 请求参数
     * @param userId 用户ID
     * @return 包含答案和来源的结果
     */
    public Map<String, Object> ragChat(RAGChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            log.info("RAG检索问题: {}", request.getQuestion());

            // 获取AI配置
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config == null) {
                throw new RuntimeException("AI配置未设置，请先在系统设置中配置AI服务");
            }
            modelName = config.getModelName();

            // Step 1: 检索相关文档
            List<ScoredDocument> scoredDocs = searchRelevantDocuments(request.getQuestion());

            if (scoredDocs.isEmpty()) {
                Map<String, Object> emptyResult = buildEmptyResult();
                aiLogService.log(userId, null, com.lawfirm.enums.AIFunctionType.LEGAL_QA,
                        request.getQuestion(), null, "未找到相关文档", null, modelName, "SUCCESS",
                        (int) (System.currentTimeMillis() - startTime), null);
                return emptyResult;
            }

            // Step 2: 构建增强上下文
            String context = buildContext(scoredDocs);

            // Step 3: 通过LLM生成答案
            String enhancedPrompt = buildRAGPrompt(request.getQuestion(), context);
            result = llmApiService.chatWithConfig(enhancedPrompt, config);

            // Step 4: 提取源信息
            List<Map<String, Object>> sources = buildSources(scoredDocs);

            Map<String, Object> response = new HashMap<>();
            response.put("answer", result);
            response.put("sources", sources);
            response.put("hasAnswer", true);
            response.put("documentCount", scoredDocs.size());
            response.put("searchMethod", "Vector Search + LLM");

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, com.lawfirm.enums.AIFunctionType.LEGAL_QA,
                    request.getQuestion(), null, result, null, modelName, status, duration, null);

            return response;

        } catch (Exception e) {
            log.error("RAG检索失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("answer", "系统暂时不可用，请稍后重试。");
            errorResult.put("error", errorMessage);
            errorResult.put("hasAnswer", false);

            // 记录错误日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, com.lawfirm.enums.AIFunctionType.LEGAL_QA,
                    request.getQuestion(), null, null, null, modelName, status, duration, errorMessage);

            return errorResult;
        }
    }

    /**
     * 检索相关文档（向量检索 + 降级方案）
     */
    private List<ScoredDocument> searchRelevantDocuments(String question) {
        try {
            // 尝试向量检索
            return searchWithVector(question);
        } catch (Exception e) {
            log.warn("向量检索失败，降级到关键词检索: {}", e.getMessage());
            // 降级到关键词检索
            return searchWithKeyword(question);
        }
    }

    /**
     * 向量检索
     */
    private List<ScoredDocument> searchWithVector(String question) {
        // 生成问题向量
        List<Double> questionVector = embeddingService.embedText(question);

        // 向量检索（Top 5，相似度阈值0.6）
        List<QdrantVectorService.SearchResult> searchResults =
                qdrantVectorService.search(questionVector, 5, 0.6);

        if (searchResults.isEmpty()) {
            log.info("向量检索未找到相关文档: question={}", question);
            return Collections.emptyList();
        }

        // 根据检索结果获取完整文档
        List<ScoredDocument> scoredDocs = new ArrayList<>();
        for (QdrantVectorService.SearchResult result : searchResults) {
            try {
                // 从payload中解析articleId
                com.google.gson.JsonObject payload = new com.google.gson.Gson()
                        .fromJson(result.payload, com.google.gson.JsonObject.class);
                long articleId = payload.get("articleId").getAsLong();

                // 获取完整文档
                KnowledgeArticle doc = knowledgeArticleRepository.findById(articleId).orElse(null);
                if (doc != null) {
                    scoredDocs.add(new ScoredDocument(doc, result.score));
                }
            } catch (Exception e) {
                log.warn("解析向量检索结果失败: {}", e.getMessage());
            }
        }

        log.info("向量检索完成: 问题={}, 检索结果数={}, 最终文档数={}",
                question, searchResults.size(), scoredDocs.size());

        return scoredDocs;
    }

    /**
     * 关键词检索（降级方案）
     */
    private List<ScoredDocument> searchWithKeyword(String question) {
        try {
            List<KnowledgeArticle> allDocs = knowledgeArticleRepository.findAll();
            List<ScoredDocument> scoredDocs = new ArrayList<>();

            String lowerQuestion = question.toLowerCase();
            for (KnowledgeArticle doc : allDocs) {
                double score = calculateKeywordRelevance(doc, lowerQuestion);
                if (score > 0.0) {
                    scoredDocs.add(new ScoredDocument(doc, score));
                }
            }

            return scoredDocs.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("关键词检索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算关键词相关性
     */
    private double calculateKeywordRelevance(KnowledgeArticle doc, String question) {
        String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
        String content = doc.getContent() != null ? doc.getContent().toLowerCase() : "";

        // 提取问题中的关键词（长度>=2的词）
        String[] keywords = question.split("\\s+");
        int matchCount = 0;
        int totalKeywords = 0;

        for (String keyword : keywords) {
            if (keyword.length() >= 2) {
                totalKeywords++;
                if (title.contains(keyword) || content.contains(keyword)) {
                    matchCount++;
                }
            }
        }

        if (totalKeywords == 0) {
            return 0.0;
        }

        return (double) matchCount / totalKeywords;
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<ScoredDocument> scoredDocs) {
        StringBuilder context = new StringBuilder();
        context.append("知识库文档（按相关性排序）:\n\n");

        for (int i = 0; i < scoredDocs.size(); i++) {
            ScoredDocument scoredDoc = scoredDocs.get(i);
            KnowledgeArticle doc = scoredDoc.document;

            context.append(String.format("[文档%d 相关度: %.2f] %s\n",
                    i + 1, scoredDoc.score, doc.getTitle()));
            context.append(String.format("分类: %s\n", doc.getCategory()));

            if (doc.getSummary() != null) {
                context.append(String.format("摘要: %s\n", doc.getSummary()));
            } else if (doc.getContent() != null) {
                String content = doc.getContent();
                context.append(String.format("内容: %s\n",
                        content.length() > 500 ? content.substring(0, 500) + "..." : content));
            }
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 构建RAG Prompt
     */
    private String buildRAGPrompt(String question, String context) {
        return "你是一个专业的法律助手。请根据以下知识库文档回答用户的问题。\n\n" +
                "【知识库文档】\n" + context + "\n" +
                "【用户问题】\n" + question + "\n\n" +
                "【回答要求】\n" +
                "1. **仅基于上述文档回答**，不要编造信息\n" +
                "2. 如果文档中没有答案，明确告知用户\n" +
                "3. 回答要准确、专业、通俗易懂\n" +
                "4. 必要时引用文档中的具体内容\n" +
                "5. 使用清晰的格式，分段和列表\n" +
                "6. 如果涉及法律条文，请引用完整\n" +
                "7. 如果涉及案例，请说明相关法律依据\n\n" +
                "请用中文回答：";
    }

    /**
     * 构建来源信息
     */
    private List<Map<String, Object>> buildSources(List<ScoredDocument> scoredDocs) {
        return scoredDocs.stream()
                .limit(3)
                .map(scoredDoc -> {
                    Map<String, Object> sourceInfo = new HashMap<>();
                    KnowledgeArticle doc = scoredDoc.document;
                    sourceInfo.put("id", doc.getId());
                    sourceInfo.put("title", doc.getTitle());
                    sourceInfo.put("category", doc.getCategory());
                    sourceInfo.put("relevanceScore", String.format("%.2f", scoredDoc.score));

                    String summary = doc.getSummary();
                    if (summary == null && doc.getContent() != null) {
                        String content = doc.getContent();
                        summary = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                    }
                    sourceInfo.put("summary", summary != null ? summary : "");

                    return sourceInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建空结果
     */
    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> emptyResult = new HashMap<>();
        emptyResult.put("answer", "未找到相关文档。请尝试其他关键词。");
        emptyResult.put("sources", List.of());
        emptyResult.put("hasAnswer", false);
        return emptyResult;
    }

    /**
     * 评分文档（用于存储文档及其相关性分数）
     */
    private static class ScoredDocument {
        final KnowledgeArticle document;
        final double score;

        ScoredDocument(KnowledgeArticle document, double score) {
            this.document = document;
            this.score = score;
        }
    }
}
