package com.lawfirm.service;

import com.lawfirm.config.HermesAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 与设备侧 Hermes Agent 通信；路径可在配置中按实际 Hermes 版本调整。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HermesAgentClient {

    private final HermesAgentProperties properties;
    private final RestTemplate restTemplate;

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public Map<String, Object> health() {
        if (!properties.isEnabled()) {
            return Map.of("enabled", false, "reachable", false, "message", "Hermes 未启用");
        }
        try {
            String url = joinUrl(properties.getHealthPath());
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> out = new HashMap<>();
            out.put("enabled", true);
            out.put("reachable", resp.getStatusCode().is2xxSuccessful());
            out.put("status", resp.getStatusCodeValue());
            out.put("body", resp.getBody());
            return out;
        } catch (Exception e) {
            log.warn("Hermes 健康检查失败: {}", e.getMessage());
            return Map.of(
                    "enabled", true,
                    "reachable", false,
                    "message", e.getMessage()
            );
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(Map<String, Object> payload) {
        String url = joinUrl(properties.getChatPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> result = new HashMap<>();
        result.put("status", resp.getStatusCodeValue());
        result.put("data", resp.getBody());
        return result;
    }

    /**
     * 将文件转发给 Hermes 分析（multipart）。若 Hermes 未配置 analyze 接口，调用方应捕获异常并降级本地 AI。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeDocument(MultipartFile file, Map<String, String> meta) {
        String url = joinUrl(properties.getAnalyzePath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.LinkedMultiValueMap<String, Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();
        try {
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
        if (meta != null) {
            meta.forEach(body::add);
        }

        HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> entity =
                new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = new HashMap<>();
            result.put("status", resp.getStatusCodeValue());
            result.put("data", resp.getBody());
            result.put("provider", "hermes");
            return result;
        } catch (ResourceAccessException e) {
            throw new com.lawfirm.exception.AIServiceException("无法连接 Hermes Agent: " + e.getMessage(), e);
        }
    }

    /**
     * 通用网关：转发任意路径与 JSON 体（供前端或后续适配 Hermes OpenAPI）。
     */
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> forward(String relativePath, HttpMethod method, String jsonBody,
                                        HttpHeaders incomingHeaders) {
        String url = joinUrl(relativePath.startsWith("/") ? relativePath : "/" + relativePath);
        HttpHeaders headers = new HttpHeaders();
        if (incomingHeaders != null && incomingHeaders.getContentType() != null) {
            headers.setContentType(incomingHeaders.getContentType());
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
        HttpHeaders outHeaders = new HttpHeaders();
        if (resp.getHeaders().getContentType() != null) {
            outHeaders.setContentType(resp.getHeaders().getContentType());
        }
        return new ResponseEntity<>(resp.getBody(), outHeaders, resp.getStatusCode());
    }

    private String joinUrl(String path) {
        String base = properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }
}
