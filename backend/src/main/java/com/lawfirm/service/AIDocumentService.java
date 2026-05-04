package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * AI文档智能识别服务
 * 实现OCR识别 + LLM要素提取的核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDocumentService {

    private final AIConfigService aiConfigService;
    private final AILogService aiLogService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBusinessLogicHandler businessLogicHandler;
    private final LLMApiService llmApiService;

    @Value("${ai.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${ai.ocr.provider:tesseract}")
    private String ocrProvider;

    /**
     * 智能识别法院文书（带业务逻辑执行）
     * 完整流程：上传→OCR识别→LLM要素提取→业务逻辑执行→返回结果
     */
    public AIDocumentRecognitionResult recognizeLegalDocument(MultipartFile file, Long userId, Long caseId) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取AI配置（如果数据库没有配置，使用默认Ollama配置）
            AIConfig aiConfig;
            try {
                aiConfig = aiConfigService.getDefaultConfig();
            } catch (Exception e) {
                log.warn("数据库中没有默认AI配置，使用默认Ollama配置: {}", e.getMessage());
                aiConfig = createDefaultOllamaConfig();
            }

            // 2. OCR识别
            String ocrText = performOCR(file);
            log.info("OCR识别完成，文本长度: {}", ocrText.length());

            // 3. LLM要素提取
            AIDocumentRecognitionResult result = extractLegalInfo(ocrText, aiConfig);

            // 4. 执行业务逻辑（根据文书类型）
            if (result.getDocumentType() != null && !result.getDocumentType().isEmpty()) {
                executeBusinessLogic(result, userId);
            }

            // 5. 记录处理时间
            result.setProcessingTime(System.currentTimeMillis() - startTime);

            // 6. 记录AI使用日志
            // TODO: 实现AI功能后启用日志记录
            /*
            aiLogService.logUsage(
                    userId,
                    caseId,
                    "DOCUMENT_RECOGNITION",
                    ocrText.length(),
                    objectMapper.writeValueAsString(result).length(),
                    aiConfig.getModelName(),
                    "SUCCESS",
                    result.getProcessingTime()
            );
            */

            return result;

        } catch (Exception e) {
            log.error("文档识别失败", e);

            // 记录失败日志
            // TODO: 实现AI功能后启用日志记录
            /*
            aiLogService.logUsage(
                    userId,
                    caseId,
                    "DOCUMENT_RECOGNITION",
                    0,
                    0,
                    "unknown",
                    "FAILED",
                    System.currentTimeMillis() - startTime
            );
            */

            throw new AIServiceException("文档识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行OCR识别
     */
    private String performOCR(MultipartFile file) throws Exception {
        if (!ocrEnabled) {
            throw new AIServiceException("OCR功能未启用");
        }

        switch (ocrProvider.toLowerCase()) {
            case "tesseract":
                return performTesseractOCR(file);
            case "deepseek":
                return performDeepSeekVisionOCR(file);
            case "baidu":
                return performBaiduOCR(file);
            case "aliyun":
                return performAliyunOCR(file);
            default:
                throw new AIServiceException("不支持的OCR提供商: " + ocrProvider);
        }
    }

    /**
     * Tesseract OCR（本地部署）
     */
    private String performTesseractOCR(MultipartFile file) throws Exception {
        log.info("开始PDF文档OCR识别，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());

        try {
            // 1. 保存临时文件
            java.io.File tempFile = java.io.File.createTempFile("ocr_", ".pdf");
            file.transferTo(tempFile);
            log.debug("临时文件已保存: {}", tempFile.getAbsolutePath());

            // 2. 优先使用PDFBox提取PDF文本（如果PDF有文本层）
            String extractedText = extractTextFromPDF(tempFile);

            // 3. 如果提取的文本太少，尝试使用Tesseract OCR
            if (extractedText == null || extractedText.trim().length() < 100) {
                log.warn("PDF文本提取失败或文本太少(长度: {})，这是扫描版PDF，需要Tesseract OCR", extractedText == null ? 0 : extractedText.length());
                extractedText = "该PDF文档似乎是扫描版，当前OCR功能未完全配置。建议：1. 使用有文本层的PDF 2. 或手动输入判决书信息";
            } else {
                log.info("PDF文本提取成功，文本长度: {}", extractedText.length());
            }

            // 4. 删除临时文件
            tempFile.delete();
            log.debug("临时文件已删除");

            return extractedText;

        } catch (Exception e) {
            log.error("PDF文档处理失败", e);
            throw new AIServiceException("PDF文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PDF中提取文本（使用Apache PDFBox）
     */
    private String extractTextFromPDF(java.io.File pdfFile) throws Exception {
        log.debug("开始使用PDFBox提取文本");

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile)) {
            StringBuilder text = new StringBuilder();

            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                text.append(pageText).append("\n");
            }

            String result = text.toString().trim();
            log.debug("PDFBox提取完成，页数: {}, 文本长度: {}", document.getNumberOfPages(), result.length());
            return result;
        }
    }

    /**
     * 百度OCR API
     */
    private String performBaiduOCR(MultipartFile file) throws Exception {
        // TODO: 集成百度OCR API
        log.warn("百度OCR尚未实现");
        return "";
    }

    /**
     * 阿里云OCR API
     */
    private String performAliyunOCR(MultipartFile file) throws Exception {
        // TODO: 集成阿里云OCR API
        log.warn("阿里云OCR尚未实现");
        return "";
    }

    /**
     * DeepSeek Vision API（图片识别）
     * 使用DeepSeek视觉模型进行OCR识别
     */
    private String performDeepSeekVisionOCR(MultipartFile file) throws Exception {
        log.info("开始使用DeepSeek Vision API进行OCR识别，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());

        try {
            // 1. 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                // 如果是PDF，先尝试提取文本
                if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                    return extractTextFromPDF(file);
                }
                throw new AIServiceException("不支持的文件类型，仅支持图片和PDF");
            }

            // 2. 对于PDF文件，先尝试提取文本层
            if (contentType.equals("application/pdf")) {
                String pdfText = extractTextFromPDF(file);
                if (pdfText != null && pdfText.trim().length() > 100) {
                    log.info("PDF文本层提取成功，文本长度: {}", pdfText.length());
                    return pdfText;
                }
                log.warn("PDF文本层提取失败或文本太少，尝试使用Vision API");
                // TODO: 实现将PDF转换为图片后调用Vision API
                throw new AIServiceException("扫描版PDF暂不支持，请使用有文本层的PDF或图片格式");
            }

            // 3. 转换图片为Base64
            byte[] imageBytes = file.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            log.debug("图片已转换为Base64，长度: {}", base64Image.length());

            // 4. 构建OCR提示词
            String ocrPrompt = "请仔细识别图片中的所有文字内容，包括中文、英文、数字、标点符号等。" +
                    "如果是法律文书，请特别注意：\n" +
                    "1. 案号的准确性\n" +
                    "2. 当事人姓名、公司名称\n" +
                    "3. 日期格式\n" +
                    "4. 金额数字\n" +
                    "5. 法律条文引用\n\n" +
                    "请按照原文的格式和排版输出识别结果，保持段落结构和换行。";

            // 5. 调用DeepSeek Vision API
            log.info("调用DeepSeek Vision API进行文字识别...");
            String ocrResult = llmApiService.visionWithDeepSeek(ocrPrompt, base64Image);

            log.info("DeepSeek Vision API识别成功，文本长度: {}", ocrResult.length());
            return ocrResult;

        } catch (Exception e) {
            log.error("DeepSeek Vision API调用失败", e);
            throw new AIServiceException("DeepSeek Vision API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PDF文件中提取文本（支持MultipartFile）
     */
    private String extractTextFromPDF(MultipartFile file) throws Exception {
        log.debug("开始使用PDFBox提取PDF文本");

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            StringBuilder text = new StringBuilder();

            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                text.append(pageText).append("\n");
            }

            String result = text.toString().trim();
            log.debug("PDFBox提取完成，页数: {}, 文本长度: {}", document.getNumberOfPages(), result.length());
            return result;
        }
    }

    /**
     * 使用LLM提取法律文书关键信息
     */
    private AIDocumentRecognitionResult extractLegalInfo(String ocrText, AIConfig aiConfig) throws Exception {
        String prompt = buildExtractionPrompt(ocrText);

        // 调用LLM API
        String llmResponse = callLLM(prompt, aiConfig);

        // 解析LLM返回的JSON
        return parseLLMResponse(llmResponse);
    }

    /**
     * 构建要素提取Prompt（深度优化版 - 增强文书类型识别准确性）
     */
    private String buildExtractionPrompt(String ocrText) {
        return String.format(
            "你是一个专业的法律文书信息提取助手。请从以下法院文书中提取关键信息，以JSON格式返回。\n\n" +
            "## 任务说明\n" +
            "1. **精准识别文书类型**：根据文书的**格式结构**和**关键词**进行判断\n" +
            "2. **提取关键信息**：根据文书类型提取对应的关键信息\n" +
            "3. **字段验证**：确保提取的信息符合法律文书的规范\n\n" +
            "## 文书类型识别规则\n" +
            "**判决书**：标题为\"民事判决书\"/\"刑事判决书\"/\"行政判决书\"，包含\"原告\"\"被告\"\"判决如下\"\"判决如下\"\n" +
            "**裁定书**：标题为\"民事裁定书\"/\"刑事裁定书\"/\"行政裁定书\"，包含\"裁定如下\"\"本院裁定\"\n" +
            "**起诉状**：包含\"民事起诉状\"\"诉讼请求\"\"事实与理由\"\"原告\"\"被告\"\n" +
            "**答辩状**：包含\"民事答辩状\"\"答辩意见\"\"答辩人\"\"被答辩人\"\n" +
            "**调解书**：包含\"民事调解书\"\"调解协议\"\"双方当事人\"\n" +
            "**传票**：包含\"传票\"\"开庭时间\"\"开庭地点\"\"案号\"\n" +
            "**通知书**：包含\"通知书\"\"通知事项\"（应诉通知书/举证通知书等）\n\n" +
            "## 需要提取的字段\n" +
            "- **caseNumber**: 案号（格式：(2024)京01民初123号）\n" +
            "- **courtName**: 法院名称（必填，如：北京市第一人民法院）\n" +
            "- **documentType**: 文书类型（必填）\n" +
            "  - 判决书、裁定书、起诉状、答辩状、调解书、传票、通知书、其他\n" +
            "- **plaintiffName**: 原告/申请人姓名或名称\n" +
            "- **defendantName**: 被告/被申请人姓名或名称\n" +
            "- **caseReason**: 案由（如：民间借贷纠纷、买卖合同纠纷）\n" +
            "- **judgmentDate**: 判决/裁定/调解日期（仅判决书/裁定书/调解书需要，格式：YYYY-MM-DD）\n" +
            "- **hearingDate**: 开庭时间（仅传票需要，格式：YYYY-MM-DD HH:mm）\n" +
            "- **hearingPlace**: 开庭地点/法庭号\n" +
            "- **judgeName**: 承办法官/审判员姓名\n" +
            "- **clerkName**: 书记员姓名\n" +
            "- **contactPhone**: 联系电话\n\n" +
            "## 字段验证规则\n" +
            "- 案号必须符合格式：(年份)法院代字民/刑/行初/终/他字第号码号\n" +
            "- 日期必须符合实际日期逻辑（月1-12，日1-31）\n" +
            "- 人名必须是中文2-10个字符\n" +
            "- 案由必须从\"民事案件案由规定\"中选择\n\n" +
            "## 文书内容\n" +
            "```\n%s\n```\n\n" +
            "## 输出要求\n" +
            "1. **严格返回JSON格式**，不要包含任何其他文字\n" +
            "2. 无法识别的字段填null\n" +
            "3. 日期格式必须严格按照示例格式：YYYY-MM-DD 或 YYYY-MM-DD HH:mm\n" +
            "4. 文书类型必须是上述列举的类型之一\n" +
            "5. 确保提取的信息准确，避免幻觉\n\n" +
            "现在请提取信息并返回JSON：",
            ocrText
        );
    }

    /**
     * 执行业务逻辑（根据文书类型路由）
     */
    private void executeBusinessLogic(AIDocumentRecognitionResult result, Long userId) {
        String docType = result.getDocumentType();

        if (docType == null || docType.isEmpty()) {
            log.warn("文书类型为空，跳过业务逻辑执行");
            return;
        }

        log.info("开始执行业务逻辑: 文书类型={}", docType);

        try {
            switch (docType) {
                case "判决书":
                    log.info("识别到判决书，执行判决书业务逻辑");
                    businessLogicHandler.handleJudgment(result, userId);
                    break;

                case "起诉状":
                    log.info("识别到起诉状，执行起诉状业务逻辑");
                    businessLogicHandler.handleComplaint(result, userId);
                    break;

                case "答辩状":
                    log.info("识别到答辩状，执行答辩状业务逻辑");
                    businessLogicHandler.handleAnswer(result, userId);
                    break;

                case "调解书":
                    log.info("识别到调解书，执行调解书业务逻辑");
                    businessLogicHandler.handleMediation(result, userId);
                    break;

                default:
                    log.info("文书类型 {} 暂不支持自动业务逻辑执行", docType);
                    break;
            }
        } catch (Exception e) {
            log.error("执行业务逻辑失败: 文书类型={}", docType, e);
            // 不抛出异常，避免影响识别结果返回
            // 前端可以根据 businessLogicExecuted 标志判断是否需要手动处理
        }
    }

    /**
     * 调用LLM API
     */
    private String callLLM(String prompt, AIConfig aiConfig) throws Exception {
        String provider = aiConfig.getProviderType().toLowerCase();

        switch (provider) {
            case "deepseek":
                return callDeepSeekAPI(prompt, aiConfig);
            case "openai":
                return callOpenAIAPI(prompt, aiConfig);
            case "qwen":
                return callQwenAPI(prompt, aiConfig);
            case "ollama":
                return callOllamaAPI(prompt, aiConfig);
            default:
                throw new AIServiceException("不支持的LLM提供商: " + provider);
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekAPI(String prompt, AIConfig config) throws Exception {
        String url = "https://api.deepseek.com/v1/chat/completions";

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName());
        requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
        });
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2000);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            // 解析响应，提取content
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> choices = ((java.util.List<Map<String, Object>>) responseBody.get("choices")).get(0);
            Map<String, Object> message = (Map<String, Object>) choices.get("message");
            return (String) message.get("content");
        } else {
            throw new AIServiceException("DeepSeek API调用失败: " + response.getStatusCode());
        }
    }

    /**
     * 调用OpenAI API
     */
    private String callOpenAIAPI(String prompt, AIConfig config) throws Exception {
        // TODO: 实现OpenAI API调用
        log.warn("OpenAI API尚未实现");
        return "";
    }

    /**
     * 调用通义千问API
     */
    private String callQwenAPI(String prompt, AIConfig config) throws Exception {
        // TODO: 实现通义千问API调用
        log.warn("通义千问API尚未实现");
        return "";
    }

    /**
     * 调用本地Ollama API
     * Ollama是一个开源的大语言模型运行工具
     */
    private String callOllamaAPI(String prompt, AIConfig config) throws Exception {
        // 构建Ollama API URL，默认使用localhost:11434
        String baseUrl = config.getApiUrl() != null && !config.getApiUrl().isEmpty()
                ? config.getApiUrl()
                : "http://localhost:11434";
        String url = baseUrl + "/api/chat";

        // 构建请求体 - Ollama使用特定的API格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName() != null && !config.getModelName().isEmpty()
                ? config.getModelName()
                : "qwen2.5"); // 默认使用qwen2.5模型
        requestBody.put("stream", false); // 不使用流式响应
        requestBody.put("options", new HashMap<String, Object>() {{
            put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.1);
            put("num_predict", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);
        }});
        requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
        });

        // 设置请求头 - Ollama不需要API Key
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("调用Ollama API: {}, 模型: {}", url, requestBody.get("model"));

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // 解析响应，提取content
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);

                // Ollama的响应格式: {"model": "...", "created_at": "...", "message": {"role": "...", "content": "..."}, "done": true}
                Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                String content = (String) message.get("content");

                log.info("Ollama API调用成功，返回内容长度: {}", content.length());
                return content;
            } else {
                throw new AIServiceException("Ollama API调用失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用Ollama API失败", e);
            throw new AIServiceException("调用Ollama API失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析LLM响应
     */
    private AIDocumentRecognitionResult parseLLMResponse(String response) throws Exception {
        try {
            // 尝试直接解析JSON
            AIDocumentRecognitionResult result = objectMapper.readValue(response, AIDocumentRecognitionResult.class);

            // 验证和修正提取的字段
            validateAndCorrectFields(result);

            return result;
        } catch (Exception e) {
            // 如果解析失败，尝试提取JSON部分
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = response.substring(jsonStart, jsonEnd + 1);
                AIDocumentRecognitionResult result = objectMapper.readValue(jsonPart, AIDocumentRecognitionResult.class);

                // 验证和修正提取的字段
                validateAndCorrectFields(result);

                return result;
            }

            throw new AIServiceException("无法解析LLM响应: " + response);
        }
    }

    /**
     * 验证和修正提取的字段（深度优化版）
     */
    private void validateAndCorrectFields(AIDocumentRecognitionResult result) {
        // 1. 验证案号格式
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            String caseNumber = result.getCaseNumber().trim();
            // 移除案号中的多余空格和特殊字符
            caseNumber = caseNumber.replaceAll("\\s+", "");
            result.setCaseNumber(caseNumber);

            // 验证案号格式
            if (!caseNumber.matches(".*\\(\\d{4}\\).*第.*号")) {
                log.warn("案号格式可能不正确: {}", caseNumber);
            }
        }

        // 2. 验证日期格式
        if (result.getJudgmentDate() != null && !result.getJudgmentDate().trim().isEmpty()) {
            String judgmentDate = result.getJudgmentDate().trim();
            // 尝试修正常见的日期格式错误
            if (judgmentDate.matches("\\d{4}年\\d{1,2}月\\d{1,2}日")) {
                judgmentDate = judgmentDate.replaceAll("年", "-")
                                            .replaceAll("月", "-")
                                            .replaceAll("日", "");
                result.setJudgmentDate(judgmentDate);
            }
        }

        // 3. 验证人名格式
        if (result.getPlaintiffName() != null && result.getPlaintiffName().length() > 50) {
            log.warn("原告名称过长，可能提取错误: {}", result.getPlaintiffName());
            result.setPlaintiffName(result.getPlaintiffName().substring(0, 50));
        }

        if (result.getDefendantName() != null && result.getDefendantName().length() > 50) {
            log.warn("被告名称过长，可能提取错误: {}", result.getDefendantName());
            result.setDefendantName(result.getDefendantName().substring(0, 50));
        }

        // 4. 验证文书类型
        if (result.getDocumentType() != null) {
            String docType = result.getDocumentType().trim();
            // 标准化文书类型名称
            if (docType.contains("判决")) {
                result.setDocumentType("判决书");
            } else if (docType.contains("裁定")) {
                result.setDocumentType("裁定书");
            } else if (docType.contains("起诉") && docType.contains("状")) {
                result.setDocumentType("起诉状");
            } else if (docType.contains("答辩") && docType.contains("状")) {
                result.setDocumentType("答辩状");
            } else if (docType.contains("调解")) {
                result.setDocumentType("调解书");
            } else if (docType.contains("传票")) {
                result.setDocumentType("传票");
            } else if (docType.contains("通知")) {
                result.setDocumentType("通知书");
            } else {
                result.setDocumentType(docType);
            }
        }

        // 5. 记录验证日志
        log.info("字段验证完成: 案号={}, 文书类型={}",
            result.getCaseNumber(), result.getDocumentType());
    }

    /**
     * 创建默认的Ollama配置（当数据库中没有配置时使用）
     */
    private AIConfig createDefaultOllamaConfig() {
        AIConfig config = new AIConfig();
        config.setProviderType("ollama");
        config.setApiUrl("http://localhost:11434");
        config.setModelName("qwen3:8b");
        config.setMaxTokens(2000);
        config.setTemperature(0.1);
        return config;
    }
}