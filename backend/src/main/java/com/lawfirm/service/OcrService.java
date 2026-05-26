package com.lawfirm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @deprecated 请使用 {@link AIDocumentService} / POST /api/ai/documents/recognize
 */
@Deprecated
@Slf4j
@Service
public class OcrService {
    public String recognizeDocument(MultipartFile file, String type) {
        log.info("开始OCR识别: 文件名={}, 大小={}, 类型={}",
                file.getOriginalFilename(), file.getSize(), type);
        
        return String.format(
                "OCR识别结果（模拟）\n" +
                "文件名: %s\n" +
                "文件类型: %s\n" +
                "文件大小: %d bytes\n\n" +
                "识别内容:\n" +
                "这是一段模拟的OCR识别结果。\n" +
                "要启用真实OCR功能，需要：\n" +
                "1. 配置PaddleOCR本地服务\n" +
                "2. 或接入云端OCR API（百度/腾讯/阿里）\n" +
                "3. 在application.yml中配置API密钥\n\n" +
                "当前OCR接口已可用，返回模拟结果供前端集成测试。",
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
    }
}
