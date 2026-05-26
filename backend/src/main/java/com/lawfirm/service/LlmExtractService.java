package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.OcrExtractRequest;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.enums.AIModelUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM智能提取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmExtractService {

    private final AIModelRoutingService aimodelRoutingService;
    private final LLMApiService llmApiService;
    private final AILogService aiLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从OCR文本中提取法律要素
     */
    public Map<String, Object> extractLegalElements(OcrExtractRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        String modelName = "";
        String status = "SUCCESS";
        String errorMessage = null;
        String result = null;

        try {
            AIConfig config = aimodelRoutingService.resolveForUseCase(AIModelUseCase.EXTRACT);
            modelName = config.getModelName();

            String prompt = buildExtractPrompt(request.getOcrText(), request.getDocumentType());
            String response = llmApiService.chatWithConfig(prompt, null, config);
            result = response;

            // 解析响应
            Map<String, Object> extracted = parseExtractResponse(response);

            // 记录日志
            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.OCR_RECOGNITION,
                    request.getOcrText(), null, result, null, modelName, status, duration, null);

            return extracted;

        } catch (Exception e) {
            log.error("LLM提取失败", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            int duration = (int) (System.currentTimeMillis() - startTime);
            aiLogService.log(userId, request.getCaseId(), AIFunctionType.OCR_RECOGNITION,
                    request.getOcrText(), null, null, null, modelName, status, duration, errorMessage);

            throw new RuntimeException("LLM提取失败: " + e.getMessage());
        }
    }

    /**
     * 构建提取Prompt
     */
    private String buildExtractPrompt(String ocrText, String documentType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个法律文书信息提取助手。请从以下法院文书中提取关键信息，以JSON格式返回。\n\n");
        prompt.append("需要提取的字段：\n");
        prompt.append("- caseNumber: 案号\n");
        prompt.append("- courtName: 法院名称\n");
        prompt.append("- hearingDate: 开庭时间(YYYY-MM-DD HH:mm)\n");
        prompt.append("- hearingPlace: 开庭地点/法庭号\n");
        prompt.append("- judgeName: 承办法官姓名\n");
        prompt.append("- clerkName: 书记员姓名\n");
        prompt.append("- plaintiffName: 原告姓名/名称\n");
        prompt.append("- defendantName: 被告姓名/名称\n");
        prompt.append("- caseReason: 案由\n");
        prompt.append("- contactPhone: 联系电话\n");
        prompt.append("- documentType: 文书类型(传票/判决书/裁定书/通知书/其他)\n\n");

        if (documentType != null) {
            prompt.append("文书类型提示：").append(documentType).append("\n\n");
        }

        prompt.append("文书内容：\n");
        prompt.append(ocrText);
        prompt.append("\n\n请严格返回JSON格式，无法识别的字段填null。");

        return prompt.toString();
    }

    /**
     * 解析提取响应
     */
    private Map<String, Object> parseExtractResponse(String response) {
        try {
            // 尝试直接解析JSON
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            // 如果解析失败，尝试从文本中提取JSON
            try {
                int jsonStart = response.indexOf("{");
                int jsonEnd = response.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = response.substring(jsonStart, jsonEnd + 1);
                    return objectMapper.readValue(jsonStr, Map.class);
                }
            } catch (Exception ex) {
                log.error("解析提取响应失败", ex);
            }
            // 返回原始响应
            Map<String, Object> result = new HashMap<>();
            result.put("rawResponse", response);
            return result;
        }
    }
}
