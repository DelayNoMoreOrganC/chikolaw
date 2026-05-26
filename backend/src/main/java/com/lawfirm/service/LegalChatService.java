package com.lawfirm.service;

import com.lawfirm.dto.LegalChatRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 法律问答服务
 * 基于LLMApiService提供专业的法律咨询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegalChatService {

    private final LLMApiService llmApiService;
    private final AILogService aiLogService;
    private final AIModelRoutingService aimodelRoutingService;

    /**
     * 通用法律咨询
     *
     * @param request 请求参数
     * @param userId 用户ID
     * @return AI回复
     */
    public String generalConsult(LegalChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            AIConfig config = aimodelRoutingService.resolveForUseCase(AIModelUseCase.LEGAL_CHAT);
            modelName = config.getModelName();

            String systemPrompt = buildLegalSystemPrompt();
            String userPrompt = request.getMessage();

            result = llmApiService.chatWithConfig(userPrompt, systemPrompt, config);

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, com.lawfirm.enums.AIFunctionType.LEGAL_QA,
                    userPrompt, null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("法律咨询失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, com.lawfirm.enums.AIFunctionType.LEGAL_QA,
                    request.getMessage(), null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("法律咨询失败: " + e.getMessage());
        }
    }

    /**
     * 构建法律系统Prompt
     */
    private String buildLegalSystemPrompt() {
        return "【角色定位】\n" +
                "你是一位资深律师，精通中国法律法规和司法实务。\n" +
                "你的职责是为用户提供准确、专业、实用的法律咨询建议。\n\n" +

                "【工作原则】\n" +
                "1. 准确性原则：基于现行有效的法律法规提供意见，不引用已废止的法律\n" +
                "2. 客观性原则：客观分析法律问题，不夸大或缩小法律风险\n" +
                "3. 实用性原则：提供可操作的建议，避免空洞的理论阐述\n" +
                "4. 谨慎性原则：对不确定的法律问题明确说明，避免误导用户\n" +
                "5. 保护隐私原则：不要求用户提供过多个人隐私信息\n\n" +

                "【回答结构要求】\n" +
                "1. 问题理解：简要概括你对用户问题的理解\n" +
                "2. 法律分析：\n" +
                "   - 相关法律规定（引用具体法条）\n" +
                "   - 法律关系分析\n" +
                "   - 关键法律要点\n" +
                "3. 风险提示：可能的法律风险和注意事项\n" +
                "4. 实务建议：具体的行动建议和解决方案\n" +
                "5. 温馨提示：是否建议寻求专业律师帮助\n\n" +

                "【特殊情况处理】\n" +
                "- 如果问题涉及刑事案件，必须强调建议用户尽快聘请刑辩律师\n" +
                "- 如果问题涉及重大财产权益，建议用户咨询专业律师并保留证据\n" +
                "- 如果问题信息不足，明确指出需要补充的信息\n" +
                "- 如果超出一般法律咨询范围，建议用户通过正规渠道寻求专业法律服务\n\n" +

                "请严格按照上述要求，提供专业、准确、实用的法律建议。";
    }
}
