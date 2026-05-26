package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AiChatRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI问答服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AIModelRoutingService aimodelRoutingService;
    private final LLMApiService llmApiService;
    private final AILogService aiLogService;
    private final CaseRepository caseRepository;
    private final CaseRecordService caseRecordService;
    private final CaseTimelineService caseTimelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通用法律问答
     */
    public String generalChat(String message, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            AIConfig config = aimodelRoutingService.resolveForUseCase(AIModelUseCase.GENERAL_CHAT);
            modelName = config.getModelName();

            String prompt = buildGeneralChatPrompt(message);
            result = llmApiService.chatWithConfig(prompt, null, config);

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("AI问答失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, null, AIFunctionType.LEGAL_QA,
                    message, null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("AI问答失败: " + e.getMessage());
        }
    }

    /**
     * 案件上下文问答
     */
    public String caseChat(AiChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            AIModelUseCase useCase = request.getCaseId() != null
                    ? AIModelUseCase.LEGAL_CHAT
                    : AIModelUseCase.GENERAL_CHAT;
            AIConfig config = aimodelRoutingService.resolveForUseCase(useCase);
            modelName = config.getModelName();

            // 获取案件信息
            Case caseEntity = null;
            String caseContext = "";
            if (request.getCaseId() != null) {
                caseEntity = caseRepository.findById(request.getCaseId())
                        .orElseThrow(() -> new RuntimeException("案件不存在: " + request.getCaseId()));
                caseContext = buildCaseContext(caseEntity);
            }

            // 构建Prompt（增强版 - 支持指令识别）
            String prompt = buildCaseChatPromptWithCommandSupport(request.getMessage(), caseContext);

            String response = llmApiService.chatWithConfig(prompt, null, config);

            // 解析是否包含指令
            AiCommandResult commandResult = parseAiCommand(response);

            // 如果AI识别出需要记录的指令，自动创建案件记录
            if (commandResult.hasCommand && caseEntity != null) {
                try {
                    executeAiCommand(commandResult, request.getCaseId(), userId);
                } catch (Exception e) {
                    log.warn("创建AI记录失败: {}", e.getMessage());
                    // 不影响主流程，继续返回AI回复
                }
            }

            // 返回处理后的响应（去除指令标记）
            result = commandResult.displayContent;

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, result, null, modelName, status, duration, null);

            return result;

        } catch (Exception e) {
            log.error("案件AI问答失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.CASE_ANALYSIS,
                    request.getMessage(), null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("案件AI问答失败: " + e.getMessage());
        }
    }

    /**
     * 构建通用问答Prompt（优化版 - 增强专业性和安全性）
     */
    private String buildGeneralChatPrompt(String message) {
        StringBuilder prompt = new StringBuilder();

        // 角色设定
        prompt.append("【角色定位】\n");
        prompt.append("你是一位资深律师，精通中国法律法规和司法实务。\n");
        prompt.append("你的职责是为用户提供准确、专业、实用的法律咨询建议。\n\n");

        // 工作原则
        prompt.append("【工作原则】\n");
        prompt.append("1. 准确性原则：基于现行有效的法律法规提供意见，不引用已废止的法律\n");
        prompt.append("2. 客观性原则：客观分析法律问题，不夸大或缩小法律风险\n");
        prompt.append("3. 实用性原则：提供可操作的建议，避免空洞的理论阐述\n");
        prompt.append("4. 谨慎性原则：对不确定的法律问题明确说明，避免误导用户\n");
        prompt.append("5. 保护隐私原则：不要求用户提供过多个人隐私信息\n\n");

        // 回答结构要求
        prompt.append("【回答结构要求】\n");
        prompt.append("1. 问题理解：简要概括你对用户问题的理解\n");
        prompt.append("2. 法律分析：\n");
        prompt.append("   - 相关法律规定（引用具体法条）\n");
        prompt.append("   - 法律关系分析\n");
        prompt.append("   - 关键法律要点\n");
        prompt.append("3. 风险提示：可能的法律风险和注意事项\n");
        prompt.append("4. 实务建议：具体的行动建议和解决方案\n");
        prompt.append("5. 温馨提示：是否建议寻求专业律师帮助\n\n");

        // 用户问题
        prompt.append("【用户问题】\n");
        prompt.append(message).append("\n\n");

        // 特殊情况处理
        prompt.append("【特殊情况处理】\n");
        prompt.append("- 如果问题涉及刑事案件，必须强调建议用户尽快聘请刑辩律师\n");
        prompt.append("- 如果问题涉及重大财产权益，建议用户咨询专业律师并保留证据\n");
        prompt.append("- 如果问题信息不足，明确指出需要补充的信息\n");
        prompt.append("- 如果超出一般法律咨询范围，建议用户通过正规渠道寻求专业法律服务\n\n");

        prompt.append("请严格按照上述要求，提供专业、准确、实用的法律建议。\n");

        return prompt.toString();
    }

    /**
     * 构建案件问答Prompt（优化版 - 增强案件分析能力）
     */
    private String buildCaseChatPrompt(String message, String caseContext) {
        StringBuilder prompt = new StringBuilder();

        // 角色设定
        prompt.append("【角色定位】\n");
        prompt.append("你是一位资深律师，正在与当事人讨论其案件。\n");
        prompt.append("你的职责是基于案件信息，提供专业、深入、可操作的法律意见。\n\n");

        // 工作原则
        prompt.append("【工作原则】\n");
        prompt.append("1. 深度分析：基于案件事实和法律规定进行深入分析\n");
        prompt.append("2. 结果导向：关注案件的实务操作和可能结果\n");
        prompt.append("3. 风险管控：明确提示案件中的风险点和应对策略\n");
        prompt.append("4. 策略思维：提供多角度的办案思路和策略选择\n");
        prompt.append("5. 专业谨慎：对不确定的因素保持专业谨慎态度\n\n");

        // 回答结构要求
        prompt.append("【回答结构要求】\n");
        prompt.append("1. 案件要点概括：提炼案件的关键事实和法律要点\n");
        prompt.append("2. 法律关系分析：\n");
        prompt.append("   - 当事人之间的法律关系\n");
        prompt.append("   - 争议焦点的法律分析\n");
        prompt.append("   - 适用的法律规定\n");
        prompt.append("3. 证据分析：\n");
        prompt.append("   - 现有证据的证明力评估\n");
        prompt.append("   - 需要补充的证据清单\n");
        prompt.append("   - 证据收集建议\n");
        prompt.append("4. 程序建议：\n");
        prompt.append("   - 下一步的诉讼/仲裁程序\n");
        prompt.append("   - 关键时间节点提醒\n");
        prompt.append("   - 程序风险提示\n");
        prompt.append("5. 实体建议：\n");
        prompt.append("   - 诉讼策略建议\n");
        prompt.append("   - 和解/调解的可行性\n");
        prompt.append("   - 胜诉可能性评估\n");
        prompt.append("6. 风险提示：案件中的主要风险点和防范措施\n");
        prompt.append("7. 行动建议：具体、可操作的下一步行动清单\n\n");

        // 案件信息
        if (caseContext != null && !caseContext.isEmpty()) {
            prompt.append("【案件基本信息】\n");
            prompt.append(caseContext).append("\n\n");
        }

        // 用户问题
        prompt.append("【用户问题】\n");
        prompt.append(message).append("\n\n");

        // 专业提示
        prompt.append("【专业提示】\n");
        prompt.append("- 请基于案件的具体情况，提供针对性的法律意见\n");
        prompt.append("- 对案件中的有利和不利因素都要客观分析\n");
        prompt.append("- 提供多种应对策略，并分析各自的利弊\n");
        prompt.append("- 明确指出案件的关键胜负手\n");
        prompt.append("- 对时间敏感的事项要特别提醒\n\n");

        prompt.append("请严格按照上述要求，提供深入、专业、可操作的案件法律意见。\n");

        return prompt.toString();
    }

    /**
     * 构建案件上下文
     */
    private String buildCaseContext(Case caseEntity) {
        StringBuilder context = new StringBuilder();
        context.append("案件名称：").append(caseEntity.getCaseName()).append("\n");
        context.append("案号：").append(caseEntity.getCaseNumber()).append("\n");
        context.append("案件类型：").append(caseEntity.getCaseType()).append("\n");
        context.append("案由：").append(caseEntity.getCaseReason()).append("\n");
        context.append("管辖法院：").append(caseEntity.getCourt()).append("\n");
        if (caseEntity.getSummary() != null) {
            context.append("案件简述：").append(caseEntity.getSummary()).append("\n");
        }
        context.append("案件状态：").append(caseEntity.getStatus()).append("\n");
        return context.toString();
    }

    /**
     * 构建案件问答Prompt（增强版 - 支持指令识别）
     */
    private String buildCaseChatPromptWithCommandSupport(String message, String caseContext) {
        StringBuilder prompt = new StringBuilder();

        // 角色设定
        prompt.append("【角色定位】\n");
        prompt.append("你是一位资深律师，正在与当事人讨论其案件。\n");
        prompt.append("你的职责是基于案件信息，提供专业、深入、可操作的法律意见。\n\n");

        // 工作原则
        prompt.append("【工作原则】\n");
        prompt.append("1. 深度分析：基于案件事实和法律规定进行深入分析\n");
        prompt.append("2. 结果导向：关注案件的实务操作和可能结果\n");
        prompt.append("3. 风险管控：明确提示案件中的风险点和应对策略\n");
        prompt.append("4. 策略思维：提供多角度的办案思路和策略选择\n");
        prompt.append("5. 专业谨慎：对不确定的因素保持专业谨慎态度\n\n");

        // **新增：指令识别能力**
        prompt.append("【指令识别能力】\n");
        prompt.append("当用户提出以下类型的请求时，你需要识别并在回复末尾以特定格式标记：\n");
        prompt.append("1. 记录类指令：\"记录这个\"、\"记下来\"、\"保存到案件\"、\"添加到日志\"等\n");
        prompt.append("2. 待办类指令：\"创建待办\"、\"添加提醒\"、\"设个闹钟\"等\n");
        prompt.append("3. 阶段变更类指令：\"案件进入下一阶段\"、\"更新状态\"等\n\n");

        prompt.append("标记格式（JSON，独立一行）：\n");
        prompt.append("```COMMAND\n");
        prompt.append("{\n");
        prompt.append("  \"command\": \"record\" | \"todo\" | \"stage\",\n");
        prompt.append("  \"content\": \"要记录的具体内容\",\n");
        prompt.append("  \"title\": \"记录标题（如果是record类型）\",\n");
        prompt.append("  \"stage\": \"目标阶段（如果是stage类型）\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        // 回答结构要求
        prompt.append("【回答结构要求】\n");
        prompt.append("1. 案件要点概括：提炼案件的关键事实和法律要点\n");
        prompt.append("2. 法律关系分析：\n");
        prompt.append("   - 当事人之间的法律关系\n");
        prompt.append("   - 争议焦点的法律分析\n");
        prompt.append("   - 适用的法律规定\n");
        prompt.append("3. 证据分析：\n");
        prompt.append("   - 现有证据的证明力评估\n");
        prompt.append("   - 需要补充的证据清单\n");
        prompt.append("   - 证据收集建议\n");
        prompt.append("4. 程序建议：\n");
        prompt.append("   - 下一步的诉讼/仲裁程序\n");
        prompt.append("   - 关键时间节点提醒\n");
        prompt.append("   - 程序风险提示\n");
        prompt.append("5. 实体建议：\n");
        prompt.append("   - 诉讼策略建议\n");
        prompt.append("   - 和解/调解的可行性\n");
        prompt.append("   - 胜诉可能性评估\n");
        prompt.append("6. 风险提示：案件中的主要风险点和防范措施\n");
        prompt.append("7. 行动建议：具体、可操作的下一步行动清单\n\n");

        // 案件信息
        if (caseContext != null && !caseContext.isEmpty()) {
            prompt.append("【案件基本信息】\n");
            prompt.append(caseContext).append("\n\n");
        }

        // 用户问题
        prompt.append("【用户问题】\n");
        prompt.append(message).append("\n\n");

        // 专业提示
        prompt.append("【专业提示】\n");
        prompt.append("- 请基于案件的具体情况，提供针对性的法律意见\n");
        prompt.append("- 对案件中的有利和不利因素都要客观分析\n");
        prompt.append("- 提供多种应对策略，并分析各自的利弊\n");
        prompt.append("- 明确指出案件的关键胜负手\n");
        prompt.append("- 对时间敏感的事项要特别提醒\n");
        prompt.append("- **如果用户要求记录某项内容，请务必在回复末尾添加COMMAND标记**\n\n");

        prompt.append("请严格按照上述要求，提供深入、专业、可操作的案件法律意见。\n");

        return prompt.toString();
    }

    /**
     * AI指令识别结果
     */
    @lombok.Data
    public static class AiCommandResult {
        private boolean hasCommand;
        private String commandType; // "record", "todo", "stage"
        private String recordContent; // 要记录的内容
        private String recordTitle; // 记录标题
        private String targetStage; // 目标阶段
        private String displayContent; // 展示给用户的内容（去除指令标记）
    }

    /**
     * 解析AI响应中的指令
     */
    private AiCommandResult parseAiCommand(String aiResponse) {
        AiCommandResult result = new AiCommandResult();
        result.hasCommand = false;
        result.displayContent = aiResponse;

        try {
            // 查找COMMAND标记
            int commandStart = aiResponse.indexOf("```COMMAND");
            if (commandStart == -1) {
                return result;
            }

            int commandEnd = aiResponse.indexOf("```", commandStart + 10);
            if (commandEnd == -1) {
                return result;
            }

            // 提取JSON
            String jsonStr = aiResponse.substring(commandStart + 10, commandEnd).trim();
            log.info("检测到AI指令: {}", jsonStr);

            // 解析JSON
            JsonNode commandJson = objectMapper.readTree(jsonStr);
            String command = commandJson.path("command").asText();

            result.hasCommand = true;
            result.commandType = command;
            result.recordContent = commandJson.path("content").asText();
            result.recordTitle = commandJson.path("title").asText();
            result.targetStage = commandJson.path("stage").asText();

            // 去除指令标记，保留展示内容
            result.displayContent = aiResponse.substring(0, commandStart).trim();

            log.info("AI指令解析成功: type={}, content={}", command, result.recordContent);

        } catch (Exception e) {
            log.warn("解析AI指令失败: {}", e.getMessage());
            // 解析失败不影响主流程，返回原始内容
        }

        return result;
    }

    /**
     * 执行AI识别的指令
     */
    private void executeAiCommand(AiCommandResult commandResult, Long caseId, Long userId) {
        try {
            switch (commandResult.commandType) {
                case "record":
                    // 创建案件记录
                    createCaseRecordFromAi(caseId, commandResult, userId);
                    break;
                case "todo":
                    // 创建待办事项
                    createTodoFromAi(caseId, commandResult, userId);
                    break;
                case "stage":
                    // 更新案件阶段
                    updateCaseStageFromAi(caseId, commandResult, userId);
                    break;
                default:
                    log.warn("未知的AI指令类型: {}", commandResult.commandType);
            }
        } catch (Exception e) {
            log.error("执行AI指令失败: type={}, error={}", commandResult.commandType, e.getMessage());
            throw e;
        }
    }

    /**
     * 从AI指令创建案件记录
     */
    private void createCaseRecordFromAi(Long caseId, AiCommandResult commandResult, Long userId) {
        try {
            com.lawfirm.dto.CaseRecordDTO dto = new com.lawfirm.dto.CaseRecordDTO();
            dto.setTitle(commandResult.recordTitle != null && !commandResult.recordTitle.isEmpty()
                ? commandResult.recordTitle
                : "AI助手记录");
            dto.setContent(commandResult.recordContent);
            dto.setRecordDate(java.time.LocalDate.now());
            dto.setWorkHours(java.math.BigDecimal.ZERO);
            dto.setStage("AI记录"); // 默认阶段

            com.lawfirm.entity.CaseRecord record = caseRecordService.create(dto, caseId, userId);

            // 同时添加到Timeline
            caseTimelineService.createSystemTimeline(
                caseId,
                "AI助手记录",
                "AI助手已自动创建记录：" + (commandResult.recordTitle != null ? commandResult.recordTitle : "AI记录")
            );

            log.info("AI记录创建成功: recordId={}, caseId={}", record.getId(), caseId);
        } catch (Exception e) {
            log.error("创建AI案件记录失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从AI指令创建待办事项
     */
    private void createTodoFromAi(Long caseId, AiCommandResult commandResult, Long userId) {
        try {
            // 这里需要注入TodoService，暂时只记录日志
            log.info("AI识别到待办创建指令: content={}", commandResult.recordContent);
            // TODO: 实现待办创建功能
        } catch (Exception e) {
            log.error("创建AI待办失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从AI指令更新案件阶段
     */
    private void updateCaseStageFromAi(Long caseId, AiCommandResult commandResult, Long userId) {
        try {
            // 这里需要注入CaseStageService，暂时只记录日志
            log.info("AI识别到阶段变更指令: stage={}", commandResult.targetStage);
            // TODO: 实现阶段更新功能
        } catch (Exception e) {
            log.error("更新AI案件阶段失败: {}", e.getMessage());
            throw e;
        }
    }
}
