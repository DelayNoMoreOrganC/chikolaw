package com.lawfirm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 从常见办公文件中提取纯文本，供 LLM 分析（任意类型录入的基础设施）。
 */
@Slf4j
@Service
public class DocumentTextExtractService {

    public String extractText(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (name.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            }
        } catch (Exception e) {
            log.warn("文本提取失败: {}", e.getMessage());
        }
        return null;
    }

    public boolean isTextExtractable(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")
                || lower.endsWith(".docx");
    }
}
