package com.lawfirm.controller;

import com.lawfirm.service.OcrService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @deprecated 请使用 POST /api/ai/documents/recognize
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {
    private final OcrService ocrService;

    @PostMapping("/recognize")
    public Result<String> recognizeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "document") String type,
            javax.servlet.http.HttpServletResponse response) {
        response.setHeader("Warning", "299 - \"Deprecated: use POST /api/ai/documents/recognize\"");
        try {
            log.warn("调用已废弃接口 /api/ocr/recognize，请迁移至 /api/ai/documents/recognize");
            log.info("OCR识别请求: 文件名={}, 类型={}", file.getOriginalFilename(), type);
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            String result = ocrService.recognizeDocument(file, type);
            return Result.success(result);
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            return Result.error("OCR识别失败: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OCR服务运行正常");
    }
}
