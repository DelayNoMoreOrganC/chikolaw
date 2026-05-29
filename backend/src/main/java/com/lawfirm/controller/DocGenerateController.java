package com.lawfirm.controller;

import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.dto.TextToDocxRequest;
import com.lawfirm.service.DocGenerateService;
import com.lawfirm.service.LegalDocumentDocxService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文书生成控制器
 */
@RestController
@RequestMapping("ai/generate-doc")
@RequiredArgsConstructor
public class DocGenerateController {

    private final DocGenerateService docGenerateService;
    private final LegalDocumentDocxService legalDocumentDocxService;
    private final SecurityUtil securityUtil;

    /**
     * 生成法律文书
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER')")
    public Result<String> generateDocument(@RequestBody DocGenerateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        String result = docGenerateService.generateDocument(request, userId);
        return Result.success(result);
    }

    @PostMapping("/export-docx")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public ResponseEntity<byte[]> exportDocx(@Valid @RequestBody TextToDocxRequest request) {
        byte[] docx = legalDocumentDocxService.textToDocx(request.getTitle(), request.getContent());
        String name = request.getFileName();
        if (name == null || name.isBlank()) {
            name = "法律文书.docx";
        } else if (!name.toLowerCase().endsWith(".docx")) {
            name = name + ".docx";
        }
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docx);
    }
}
