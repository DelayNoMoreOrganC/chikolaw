package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.config.EmbeddedAgentProperties;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统内 Agent 运行时：卷宗分析统一入口，按配置选择 builtin / OpenClaw / Hermes。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddedAgentService {

    private final EmbeddedAgentProperties properties;
    private final AIDocumentService aiDocumentService;
    private final DocumentTextExtractService textExtractService;
    private final OpenClawAgentClient openClawAgentClient;
    private final HermesAgentClient hermesAgentClient;
    private final ObjectMapper objectMapper;

    public Map<String, Object> runtimeStatus() {
        Map<String, Object> status = new HashMap<>();
        String provider = properties.getProvider() != null
                ? properties.getProvider().toLowerCase() : "builtin";
        status.put("configuredProvider", provider);
        status.put("builtinAvailable", true);
        status.put("openclawReachable", openClawAgentClient.isReachable());
        status.put("hermesEnabled", hermesAgentClient.isEnabled());
        if (hermesAgentClient.isEnabled()) {
            status.put("hermes", hermesAgentClient.health());
        }
        status.put("activeProvider", resolveActiveProvider(provider));
        return status;
    }

    /**
     * 分析上传文件，返回识别结果与实际使用的引擎 id。
     */
    public AnalysisResult analyzeDocument(MultipartFile file, Long userId, Long caseId,
                                          String userRemark) {
        String configured = properties.getProvider() != null
                ? properties.getProvider().toLowerCase() : "builtin";
        String active = resolveActiveProvider(configured);

        try {
            if ("hermes".equals(active) && hermesAgentClient.isEnabled()) {
                Map<String, String> meta = new HashMap<>();
                if (caseId != null) {
                    meta.put("caseId", String.valueOf(caseId));
                }
                if (userRemark != null) {
                    meta.put("remark", userRemark);
                }
                Map<String, Object> hermesResp = hermesAgentClient.analyzeDocument(file, meta);
                AIDocumentRecognitionResult r = mapHermes(hermesResp, file.getOriginalFilename());
                return new AnalysisResult(r, "hermes");
            }

            if ("openclaw".equals(active)) {
                String text = extractPlainText(file, userId, caseId);
                AIDocumentRecognitionResult r = openClawAgentClient.extractFromText(text);
                return new AnalysisResult(r, "openclaw");
            }
        } catch (Exception e) {
            log.warn("Agent [{}] 分析失败，降级 builtin: {}", active, e.getMessage());
        }

        AIDocumentRecognitionResult builtin = analyzeBuiltin(file, userId, caseId);
        return new AnalysisResult(builtin, "builtin");
    }

    private String resolveActiveProvider(String configured) {
        switch (configured) {
            case "hermes":
                if (hermesAgentClient.isEnabled()) {
                    Map<String, Object> h = hermesAgentClient.health();
                    if (Boolean.TRUE.equals(h.get("reachable"))) {
                        return "hermes";
                    }
                }
                break;
            case "openclaw":
                if (openClawAgentClient.isReachable()) {
                    return "openclaw";
                }
                break;
            case "builtin":
            default:
                return "builtin";
        }
        return "builtin";
    }

    private AIDocumentRecognitionResult analyzeBuiltin(MultipartFile file, Long userId, Long caseId) {
        try {
            if (textExtractService.isTextExtractable(file.getOriginalFilename())) {
                String text = textExtractService.extractText(file);
                if (text != null && text.trim().length() > 20) {
                    return aiDocumentService.recognizeFromText(text, userId, caseId);
                }
            }
            return aiDocumentService.recognizeLegalDocument(file, userId, caseId, false);
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("builtin 分析失败: {}", e.getMessage(), e);
            throw new AIServiceException("卷宗文书分析失败: " + e.getMessage(), e);
        }
    }

    private String extractPlainText(MultipartFile file, Long userId, Long caseId) throws Exception {
        if (textExtractService.isTextExtractable(file.getOriginalFilename())) {
            String text = textExtractService.extractText(file);
            if (text != null && text.trim().length() > 20) {
                return text;
            }
        }
        AIDocumentRecognitionResult partial =
                aiDocumentService.recognizeLegalDocument(file, userId, caseId, false);
        if (partial.getOcrText() != null && !partial.getOcrText().isBlank()) {
            return partial.getOcrText();
        }
        throw new com.lawfirm.exception.AIServiceException("无法从文件提取文本");
    }

    @SuppressWarnings("unchecked")
    private AIDocumentRecognitionResult mapHermes(Map<String, Object> hermesResp, String filename) {
        Object data = hermesResp.get("data");
        if (data instanceof Map) {
            return objectMapper.convertValue(data, AIDocumentRecognitionResult.class);
        }
        AIDocumentRecognitionResult r = new AIDocumentRecognitionResult();
        r.setDocumentType("其他");
        r.setOcrText("Hermes: " + filename);
        return r;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AnalysisResult {
        private AIDocumentRecognitionResult recognition;
        private String provider;
    }
}
