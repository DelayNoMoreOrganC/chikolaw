package com.lawfirm.service;

import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.enums.AIModelUseCase;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.util.DocumentTypeAliasResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI文书生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocGenerateService {

    private final AIModelRoutingService aimodelRoutingService;
    private final LLMApiService llmApiService;
    private final AILogService aiLogService;
    private final CaseRepository caseRepository;

    /**
     * 生成法律文书
     */
    public String generateDocument(DocGenerateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            AIConfig config = aimodelRoutingService.resolveForUseCase(AIModelUseCase.LEGACY_DOCUMENT);
            modelName = config.getModelName();

            // 获取案件信息
            Case caseEntity = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            String documentType = DocumentTypeAliasResolver.normalize(request.getDocumentType());
            request.setDocumentType(documentType);

            // 构建Prompt
            String prompt = buildDocumentPrompt(caseEntity, documentType,
                    request.getCustomPrompt(), request.getAdditionalContext());

            String response = llmApiService.chatWithConfig(prompt, null, config);
            result = response;

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    prompt, null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("文书生成失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.DOCUMENT_GENERATION,
                    null, null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("文书生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建文书生成Prompt（优化版 - 增强专业性和结构化）
     */
    private String buildDocumentPrompt(Case caseEntity, String documentType,
                                       String customPrompt, String additionalContext) {
        StringBuilder prompt = new StringBuilder();

        // 通用Prompt头部 - 角色设定和质量要求
        prompt.append("你是一位资深律师，精通中国法律实务和文书写作规范。\n");
        prompt.append("请严格按照以下要求起草法律文书：\n");
        prompt.append("1. 格式规范：符合最新法院文书格式标准\n");
        prompt.append("2. 语言专业：使用准确的法律术语，简洁明了\n");
        prompt.append("3. 逻辑清晰：层次分明，论证充分\n");
        prompt.append("4. 内容完整：包含所有必要要素\n\n");

        // 案件基础信息
        prompt.append("=== 案件基础信息 ===\n");
        prompt.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
        if (caseEntity.getCaseNumber() != null) {
            prompt.append("案号：").append(caseEntity.getCaseNumber()).append("\n");
        }
        prompt.append("案由：").append(caseEntity.getCaseReason() != null ? caseEntity.getCaseReason() : "待补充").append("\n");
        prompt.append("案件类型：").append(caseEntity.getCaseType()).append("\n");
        if (caseEntity.getCourt() != null) {
            prompt.append("管辖法院：").append(caseEntity.getCourt()).append("\n");
        }
        if (caseEntity.getFilingDate() != null) {
            prompt.append("立案日期：").append(caseEntity.getFilingDate()).append("\n");
        }
        if (caseEntity.getAmount() != null) {
            prompt.append("争议金额：").append(caseEntity.getAmount()).append("元\n");
        }
        if (caseEntity.getSummary() != null && !caseEntity.getSummary().isEmpty()) {
            prompt.append("案件摘要：").append(caseEntity.getSummary()).append("\n");
        }
        prompt.append("\n");

        // 根据文书类型定制Prompt
        switch (documentType) {
            case "COMPLAINT":
                prompt.append("=== 文书类型：民事起诉状 ===\n\n");
                prompt.append("【格式要求】\n");
                prompt.append("一、首部\n");
                prompt.append("  1. 文书名称：民事起诉状\n");
                prompt.append("  2. 原告信息：姓名、性别、出生日期、民族、身份证号、住所地、联系电话\n");
                prompt.append("  3. 被告信息：姓名/名称、住所地、联系电话（单位需注明法定代表人）\n");
                prompt.append("  4. 诉讼代理人信息（如有）\n\n");
                prompt.append("二、诉讼请求（分项列举，明确具体）\n");
                prompt.append("  1. 请求判令被告...（具体请求）\n");
                prompt.append("  2. 请求判令被告承担诉讼费用\n");
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    prompt.append("【用户补充诉讼请求】\n").append(customPrompt).append("\n\n");
                }
                prompt.append("三、事实与理由\n");
                prompt.append("  1. 案件事实（按时间顺序，客观陈述）\n");
                prompt.append("  2. 争议焦点\n");
                prompt.append("  3. 法律依据（引用具体法律条文）\n");
                prompt.append("  4. 诉讼请求的理由和依据\n\n");
                prompt.append("四、证据和证据来源\n");
                prompt.append("  列举主要证据名称、证明目的、证据来源\n\n");
                prompt.append("五、尾部\n");
                prompt.append("  1. 致送法院名称\n");
                prompt.append("  2. 原告签名（或盖章）\n");
                prompt.append("  3. 日期\n\n");
                break;

            case "DEFENSE_STATEMENT":
                prompt.append("=== 文书类型：民事答辩状 ===\n\n");
                prompt.append("【格式要求】\n");
                prompt.append("一、首部\n");
                prompt.append("  1. 文书名称：民事答辩状\n");
                prompt.append("  2. 答辩人信息（同原告信息格式）\n\n");
                prompt.append("二、答辩请求\n");
                prompt.append("  明确答辩请求，如：请求法院驳回原告全部/部分诉讼请求\n\n");
                prompt.append("三、事实与理由\n");
                prompt.append("  针对原告的诉讼请求，逐一答辩：\n");
                prompt.append("  1. 对事实的认可或否认\n");
                prompt.append("  2. 争议焦点的辩驳\n");
                prompt.append("  3. 法律适用的分析\n");
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    prompt.append("【用户补充答辩意见】\n").append(customPrompt).append("\n\n");
                }
                prompt.append("  4. 反诉请求（如有）\n\n");
                prompt.append("四、尾部（同起诉状）\n\n");
                break;

            case "BRIEF":
                prompt.append("=== 文书类型：代理词 ===\n\n");
                prompt.append("【格式要求】\n");
                prompt.append("一、首部\n");
                prompt.append("  1. 文书名称：代理词\n");
                prompt.append("  2. 尊敬的审判长/审判员\n");
                prompt.append("  3. 代理人信息及代理权限\n\n");
                prompt.append("二、前言\n");
                prompt.append("  简要说明代理人的身份、代理意见的核心观点\n\n");
                prompt.append("三、案件基本情况\n");
                prompt.append("  简明扼要地陈述案件基本事实\n\n");
                prompt.append("四、争议焦点\n");
                prompt.append("  归纳本案的争议焦点（通常2-4个）\n\n");
                prompt.append("五、代理意见（核心部分）\n");
                prompt.append("  针对每个争议焦点，分别阐述：\n");
                prompt.append("  1. 事实认定：证据分析，事实认定\n");
                prompt.append("  2. 法律适用：引用法律条文、司法解释、判例\n");
                prompt.append("  3. 法理分析：法理依据，学理观点\n");
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    prompt.append("【用户补充代理意见】\n").append(customPrompt).append("\n\n");
                }
                prompt.append("六、结语\n");
                prompt.append("  总结代理意见，提出明确的请求\n\n");
                prompt.append("七、尾部\n");
                prompt.append("  代理人签名、日期\n\n");
                break;

            case "LAWYER_LETTER":
                prompt.append("=== 文书类型：律师函 ===\n\n");
                prompt.append("【格式要求】\n");
                prompt.append("一、首部：致送单位/个人全称\n");
                prompt.append("二、事实陈述：简明说明争议或违约事实\n");
                prompt.append("三、法律分析：引用相关法律及后果\n");
                prompt.append("四、律师意见与要求：限期履行/停止侵权等\n");
                prompt.append("五、尾部：律师事务所名称、律师签名、日期\n\n");
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    prompt.append("【用户补充要求】\n").append(customPrompt).append("\n\n");
                }
                break;

            case "LEGAL_OPINION":
                prompt.append("=== 文书类型：法律意见书 ===\n\n");
                prompt.append("【格式要求】\n");
                prompt.append("一、首部\n");
                prompt.append("  1. 文书名称：法律意见书\n");
                prompt.append("  2. 致：委托人\n");
                prompt.append("  3. 日期\n\n");
                prompt.append("二、前言\n");
                prompt.append("  说明出具法律意见的背景、依据、目的\n\n");
                prompt.append("三、事实背景\n");
                prompt.append("  客观陈述相关事实（基于现有材料）\n\n");
                prompt.append("四、法律分析\n");
                prompt.append("  1. 适用的法律框架\n");
                prompt.append("  2. 关键法律问题分析\n");
                prompt.append("  3. 相关案例参考（如有）\n\n");
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    prompt.append("【用户补充咨询问题】\n").append(customPrompt).append("\n\n");
                }
                prompt.append("五、风险评估\n");
                prompt.append("  1. 法律风险\n");
                prompt.append("  2. 诉讼风险（如涉及）\n");
                prompt.append("  3. 其他风险\n\n");
                prompt.append("六、结论与建议\n");
                prompt.append("  1. 明确的法律结论\n");
                prompt.append("  2. 具体的行动建议\n");
                prompt.append("  3. 注意事项\n\n");
                prompt.append("七、声明\n");
                prompt.append("  标准免责声明（意见基于现有材料等）\n\n");
                break;

            default:
                String display = DocumentTypeAliasResolver.displayName(documentType);
                prompt.append("=== 文书类型：").append(display).append(" ===\n\n");
                prompt.append("请根据上述案件信息，起草一份").append(display).append("。\n");
                prompt.append("注意：请确保文书格式规范、内容完整、逻辑清晰。\n\n");
                break;
        }

        // 补充信息
        if (additionalContext != null && !additionalContext.isEmpty()) {
            prompt.append("=== 补充信息 ===\n").append(additionalContext).append("\n\n");
        }

        // 质量控制提示
        prompt.append("=== 质量要求 ===\n");
        prompt.append("1. 请确保内容真实、准确，不虚构事实\n");
        prompt.append("2. 法律条文引用请准确到条、款、项\n");
        prompt.append("3. 数字、日期、名称等关键信息请准确无误\n");
        prompt.append("4. 语言简洁专业，避免冗余表述\n");
        prompt.append("5. 逻辑结构清晰，层次分明\n\n");
        prompt.append("请现在开始起草上述文书，直接输出文书内容，无需额外的解释说明。\n");

        return prompt.toString();
    }
}
