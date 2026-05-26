package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.DocumentGenerateRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.exception.AIServiceException;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.util.DocumentTypeAliasResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI文书生成服务
 * 使用DeepSeek Chat API生成各类法律文书
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final LLMApiService llmApiService;
    private final AIModelRoutingService aimodelRoutingService;
    private final AILogService aiLogService;
    private final CaseRepository caseRepository;
    private final ObjectMapper objectMapper;

    /**
     * 文书类型枚举
     */
    public enum DocumentType {
        COMPLAINT("起诉状"),
        DEFENSE_STATEMENT("答辩状"),
        BRIEF("代理词"),
        LEGAL_OPINION("法律意见书"),
        LAWYER_LETTER("律师函");

        private final String description;

        DocumentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 生成法律文书
     *
     * @param request 文书生成请求
     * @param userId  用户ID
     * @return 生成的文书内容
     */
    public String generateDocument(DocumentGenerateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;
        String prompt = null;

        try {
            request.setDocumentType(DocumentTypeAliasResolver.normalize(request.getDocumentType()));

            AIConfig config = aimodelRoutingService.resolveForUseCase(
                    DocumentTypeAliasResolver.isLegacyDocumentType(request.getDocumentType())
                            ? AIModelUseCase.LEGACY_DOCUMENT
                            : AIModelUseCase.DOCUMENT);
            modelName = config.getModelName();

            // 获取案件信息
            Case caseEntity = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new AIServiceException("案件不存在"));

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(request.getDocumentType());

            // 构建用户消息
            String userMessage = buildUserMessage(caseEntity, request);

            // 记录完整的Prompt用于日志
            prompt = "System:\n" + systemPrompt + "\n\nUser:\n" + userMessage;

            // 调用当前默认配置（支持本地优先 + 失败自动降级到 DeepSeek）
            result = llmApiService.chatWithConfig(userMessage, systemPrompt, config);

            // 记录成功日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    prompt, null, result, null, modelName, status, duration, null);

            log.info("文书生成成功: 案件ID={}, 文书类型={}, 耗时={}ms",
                    request.getCaseId(), request.getDocumentType(), duration);

            return result;

        } catch (AIServiceException e) {
            log.error("文书生成失败: {}", e.getMessage(), e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    prompt, null, null, null, modelName, status, duration, errorMessage);

            throw e;
        } catch (Exception e) {
            log.error("文书生成失败: {}", e.getMessage(), e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    prompt, null, null, null, modelName, status, duration, errorMessage);

            throw new AIServiceException("文书生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建系统提示词
     * 根据文书类型返回专业的系统提示
     */
    private String buildSystemPrompt(String documentType) {
        StringBuilder systemPrompt = new StringBuilder();

        // 通用角色设定
        systemPrompt.append("你是一位资深律师，精通中国法律实务和文书写作规范。\n");
        systemPrompt.append("请严格按照以下要求起草法律文书：\n");
        systemPrompt.append("1. 格式规范：符合最新法院文书格式标准\n");
        systemPrompt.append("2. 语言专业：使用准确的法律术语，简洁明了\n");
        systemPrompt.append("3. 逻辑清晰：层次分明，论证充分\n");
        systemPrompt.append("4. 内容完整：包含所有必要要素\n");
        systemPrompt.append("5. 事实准确：基于提供的事实信息，不虚构\n");
        systemPrompt.append("6. 法律依据：引用具体的法律条文和司法解释\n\n");

        // 根据文书类型添加专门要求
        switch (documentType) {
            case "COMPLAINT":
                systemPrompt.append("【起诉状专门要求】\n");
                systemPrompt.append("- 诉讼请求要明确、具体、可执行\n");
                systemPrompt.append("- 事实与理由要按时间顺序陈述\n");
                systemPrompt.append("- 证据要列明清单和证明目的\n");
                systemPrompt.append("- 注意管辖法院的级别和地域\n");
                break;

            case "DEFENSE_STATEMENT":
                systemPrompt.append("【答辩状专门要求】\n");
                systemPrompt.append("- 针对原告的诉讼请求逐一答辩\n");
                systemPrompt.append("- 明确承认、否认或需要进一步核实的事实\n");
                systemPrompt.append("- 提出有力的答辩理由和法律依据\n");
                systemPrompt.append("- 如有反诉请求，应单独列出\n");
                break;

            case "BRIEF":
                systemPrompt.append("【代理词专门要求】\n");
                systemPrompt.append("- 尊称：使用'审判长、审判员'\n");
                systemPrompt.append("- 归纳争议焦点，逐点分析\n");
                systemPrompt.append("- 事实认定与法律适用相结合\n");
                systemPrompt.append("- 引用判例和司法解释增强说服力\n");
                systemPrompt.append("- 语言恳切但不失专业\n");
                break;

            case "LEGAL_OPINION":
                systemPrompt.append("【法律意见书专门要求】\n");
                systemPrompt.append("- 明确出具意见的依据和限制\n");
                systemPrompt.append("- 事实陈述要客观中立\n");
                systemPrompt.append("- 法律分析要全面、深入\n");
                systemPrompt.append("- 风险提示要充分、明确\n");
                systemPrompt.append("- 结论要清晰，建议要具体\n");
                systemPrompt.append("- 包含标准免责声明\n");
                break;

            case "LAWYER_LETTER":
                systemPrompt.append("【律师函专门要求】\n");
                systemPrompt.append("- 事实简明、语气庄重\n");
                systemPrompt.append("- 法律依据明确，履行期限具体\n");
                break;

            default:
                systemPrompt.append("请根据上述通用要求起草文书。\n");
        }

        return systemPrompt.toString();
    }

    /**
     * 构建用户消息
     * 根据案件信息和请求参数生成详细的Prompt
     */
    private String buildUserMessage(Case caseEntity, DocumentGenerateRequest request) {
        StringBuilder message = new StringBuilder();

        // 文书类型
        String documentTypeName = getDocumentTypeName(request.getDocumentType());
        message.append("=== 文书类型 ===\n");
        message.append(documentTypeName).append("\n\n");

        // 案件基础信息
        message.append("=== 案件基础信息 ===\n");
        message.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
        if (caseEntity.getCaseNumber() != null) {
            message.append("案号：").append(caseEntity.getCaseNumber()).append("\n");
        }
        if (caseEntity.getCaseReason() != null) {
            message.append("案由：").append(caseEntity.getCaseReason()).append("\n");
        }
        message.append("案件类型：").append(caseEntity.getCaseType()).append("\n");
        if (caseEntity.getCourt() != null) {
            message.append("管辖法院：").append(caseEntity.getCourt()).append("\n");
        }
        if (caseEntity.getFilingDate() != null) {
            message.append("立案日期：").append(caseEntity.getFilingDate()).append("\n");
        }
        if (caseEntity.getAmount() != null) {
            message.append("争议金额：").append(caseEntity.getAmount()).append("元\n");
        }
        if (caseEntity.getSummary() != null && !caseEntity.getSummary().isEmpty()) {
            message.append("案件摘要：").append(caseEntity.getSummary()).append("\n");
        }
        message.append("\n");

        // 当事人信息
        appendPartyInfo(message, "原告", request.getPlaintiff());
        appendPartyInfo(message, "被告", request.getDefendant());
        appendPartyInfo(message, "第三人", request.getThirdParty());

        // 根据文书类型添加特定内容
        switch (request.getDocumentType()) {
            case "COMPLAINT":
                appendComplaintInfo(message, request);
                break;
            case "DEFENSE_STATEMENT":
                appendDefenseInfo(message, request);
                break;
            case "BRIEF":
                appendBriefInfo(message, request);
                break;
            case "LEGAL_OPINION":
                appendLegalOpinionInfo(message, request);
                break;
        }

        // 补充信息
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isEmpty()) {
            message.append("=== 补充信息 ===\n");
            message.append(request.getAdditionalContext()).append("\n\n");
        }

        // 自定义Prompt
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            message.append("=== 特殊要求 ===\n");
            message.append(request.getCustomPrompt()).append("\n\n");
        }

        // 结束语
        message.append("=== 生成要求 ===\n");
        message.append("请根据以上信息，起草一份完整、规范的").append(documentTypeName);
        message.append("。直接输出文书内容，格式规范，内容完整，无需额外的解释说明。\n");

        return message.toString();
    }

    /**
     * 添加当事人信息
     */
    private void appendPartyInfo(StringBuilder message, String role, DocumentGenerateRequest.PartyInfo partyInfo) {
        if (partyInfo == null) {
            return;
        }

        message.append("===").append(role).append("信息===\n");
        message.append("名称：").append(partyInfo.getName()).append("\n");

        if ("PERSON".equalsIgnoreCase(partyInfo.getType())) {
            if (partyInfo.getGender() != null) {
                message.append("性别：").append(partyInfo.getGender()).append("\n");
            }
            if (partyInfo.getBirthDate() != null) {
                message.append("出生日期：").append(partyInfo.getBirthDate()).append("\n");
            }
            if (partyInfo.getNationality() != null) {
                message.append("民族：").append(partyInfo.getNationality()).append("\n");
            }
            if (partyInfo.getIdCard() != null) {
                message.append("身份证号：").append(partyInfo.getIdCard()).append("\n");
            }
        } else if ("COMPANY".equalsIgnoreCase(partyInfo.getType())) {
            if (partyInfo.getLegalRepresentative() != null) {
                message.append("法定代表人：").append(partyInfo.getLegalRepresentative()).append("\n");
            }
            if (partyInfo.getCreditCode() != null) {
                message.append("统一社会信用代码：").append(partyInfo.getCreditCode()).append("\n");
            }
        }

        if (partyInfo.getAddress() != null) {
            message.append("住所地：").append(partyInfo.getAddress()).append("\n");
        }
        if (partyInfo.getPhone() != null) {
            message.append("联系电话：").append(partyInfo.getPhone()).append("\n");
        }

        message.append("\n");
    }

    /**
     * 添加起诉状特定信息
     */
    private void appendComplaintInfo(StringBuilder message, DocumentGenerateRequest request) {
        message.append("=== 诉讼请求 ===\n");
        if (request.getClaims() != null && !request.getClaims().isEmpty()) {
            message.append(request.getClaims()).append("\n");
        } else {
            message.append("（请根据案件情况填写具体诉讼请求）\n");
        }
        message.append("\n");

        message.append("=== 事实与理由 ===\n");
        if (request.getFactsAndReasons() != null && !request.getFactsAndReasons().isEmpty()) {
            message.append(request.getFactsAndReasons()).append("\n");
        } else {
            message.append("（请按时间顺序陈述案件事实）\n");
        }
        message.append("\n");

        message.append("=== 证据清单 ===\n");
        if (request.getEvidenceList() != null && !request.getEvidenceList().isEmpty()) {
            message.append(request.getEvidenceList()).append("\n");
        } else {
            message.append("（请列明证据名称、证明目的、证据来源）\n");
        }
        message.append("\n");
    }

    /**
     * 添加答辩状特定信息
     */
    private void appendDefenseInfo(StringBuilder message, DocumentGenerateRequest request) {
        message.append("=== 答辩意见 ===\n");
        if (request.getDefenseOpinion() != null && !request.getDefenseOpinion().isEmpty()) {
            message.append(request.getDefenseOpinion()).append("\n");
        } else {
            message.append("（请针对原告的诉讼请求提出答辩意见）\n");
        }
        message.append("\n");

        // 答辩状也可能包含事实和证据
        if (request.getFactsAndReasons() != null && !request.getFactsAndReasons().isEmpty()) {
            message.append("=== 事实与理由 ===\n");
            message.append(request.getFactsAndReasons()).append("\n\n");
        }

        if (request.getEvidenceList() != null && !request.getEvidenceList().isEmpty()) {
            message.append("=== 证据清单 ===\n");
            message.append(request.getEvidenceList()).append("\n\n");
        }
    }

    /**
     * 添加代理词特定信息
     */
    private void appendBriefInfo(StringBuilder message, DocumentGenerateRequest request) {
        message.append("=== 代理意见要点 ===\n");
        if (request.getBriefPoints() != null && !request.getBriefPoints().isEmpty()) {
            message.append(request.getBriefPoints()).append("\n");
        } else {
            message.append("（请列出代理意见的核心要点）\n");
        }
        message.append("\n");

        // 代理词通常也需要事实和理由
        if (request.getFactsAndReasons() != null && !request.getFactsAndReasons().isEmpty()) {
            message.append("=== 事实与理由 ===\n");
            message.append(request.getFactsAndReasons()).append("\n\n");
        }

        if (request.getEvidenceList() != null && !request.getEvidenceList().isEmpty()) {
            message.append("=== 证据分析 ===\n");
            message.append(request.getEvidenceList()).append("\n\n");
        }
    }

    /**
     * 添加法律意见书特定信息
     */
    private void appendLegalOpinionInfo(StringBuilder message, DocumentGenerateRequest request) {
        message.append("=== 咨询问题 ===\n");
        if (request.getConsultationQuestions() != null && !request.getConsultationQuestions().isEmpty()) {
            message.append(request.getConsultationQuestions()).append("\n");
        } else {
            message.append("（请明确需要咨询的法律问题）\n");
        }
        message.append("\n");

        // 法律意见书需要详细的背景信息
        if (request.getFactsAndReasons() != null && !request.getFactsAndReasons().isEmpty()) {
            message.append("=== 事实背景 ===\n");
            message.append(request.getFactsAndReasons()).append("\n\n");
        }

        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isEmpty()) {
            message.append("=== 相关材料 ===\n");
            message.append(request.getAdditionalContext()).append("\n\n");
        }
    }

    /**
     * 获取文书类型中文名称
     */
    private String getDocumentTypeName(String documentType) {
        try {
            return DocumentType.valueOf(documentType).getDescription();
        } catch (IllegalArgumentException e) {
            return DocumentTypeAliasResolver.displayName(documentType);
        }
    }

    /**
     * 验证文书生成请求的完整性
     * 根据文书类型检查必填字段
     */
    public void validateRequest(DocumentGenerateRequest request) {
        if (request.getCaseId() == null) {
            throw new AIServiceException("案件ID不能为空");
        }

        if (request.getDocumentType() == null || request.getDocumentType().isEmpty()) {
            throw new AIServiceException("文书类型不能为空");
        }

        request.setDocumentType(DocumentTypeAliasResolver.normalize(request.getDocumentType()));

        // 根据文书类型检查必填字段
        switch (request.getDocumentType()) {
            case "COMPLAINT":
                if (request.getPlaintiff() == null || request.getPlaintiff().getName() == null) {
                    throw new AIServiceException("起诉状必须提供原告信息");
                }
                if (request.getDefendant() == null || request.getDefendant().getName() == null) {
                    throw new AIServiceException("起诉状必须提供被告信息");
                }
                break;

            case "DEFENSE_STATEMENT":
                if (request.getDefendant() == null || request.getDefendant().getName() == null) {
                    throw new AIServiceException("答辩状必须提供被告（答辩人）信息");
                }
                break;

            case "BRIEF":
                // 代理词可以不强制要求特定字段，使用案件信息即可
                break;

            case "LEGAL_OPINION":
                if (request.getConsultationQuestions() == null || request.getConsultationQuestions().isEmpty()) {
                    throw new AIServiceException("法律意见书必须明确咨询问题");
                }
                break;

            case "LAWYER_LETTER":
                break;

            default:
                throw new AIServiceException("不支持的文书类型: " + request.getDocumentType());
        }
    }
}
