package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.config.EmbeddedAgentProperties;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对接本机 OpenClaw Gateway（OpenAI 兼容 /v1/chat/completions）。
 * 需用户在设备上自行启动 OpenClaw；未启动时自动降级 builtin。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenClawAgentClient {

    private final EmbeddedAgentProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public boolean isReachable() {
        try {
            String url = normalizeBase() + "/models";
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("OpenClaw 不可达: {}", e.getMessage());
            return false;
        }
    }

    public AIDocumentRecognitionResult extractFromText(String ocrText) throws Exception {
        String prompt = buildExtractionPrompt(ocrText);
        String json = chat(prompt);
        return parseRecognition(json, ocrText);
    }

    private String chat(String prompt) throws Exception {
        EmbeddedAgentProperties.OpenClaw cfg = properties.getOpenclaw();
        String url = normalizeBase() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.getModel());
        body.put("temperature", 0.1);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是法律文书信息提取助手，只输出 JSON。"),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
            headers.setBearerAuth(cfg.getApiKey());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new AIServiceException("OpenClaw 调用失败: HTTP " + resp.getStatusCodeValue());
        }

        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("");
        }
        throw new AIServiceException("OpenClaw 响应无 content");
    }

    private AIDocumentRecognitionResult parseRecognition(String llmResponse, String ocrText) throws Exception {
        String json = extractJsonBlock(llmResponse);
        AIDocumentRecognitionResult result = objectMapper.readValue(json, AIDocumentRecognitionResult.class);
        result.setOcrText(ocrText);
        return result;
    }

    private String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String buildExtractionPrompt(String ocrText) {
        return "从以下法律文书中提取案号、法院、文书类型、原被告、案由、开庭时间等，严格返回 JSON，字段名："
                + "caseNumber,courtName,documentType,plaintiffName,defendantName,caseReason,"
                + "hearingDate,hearingPlace,judgeName,clerkName,judgmentDate。无法识别填 null。\n\n"
                + ocrText;
    }

    private String normalizeBase() {
        String base = properties.getOpenclaw().getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
