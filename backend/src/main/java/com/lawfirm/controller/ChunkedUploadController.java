package com.lawfirm.controller;

import com.lawfirm.service.ChunkedUploadService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传控制器
 * PRD要求（576行）：文件上传 ≤50MB，支持断点续传
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;

    /**
     * 兼容旧前端 {@code VITE_APP_UPLOAD_URL=/api/upload}：引导使用卷宗/识别接口，避免 HTTP 404。
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> legacySingleFileUpload() {
        return Result.error(
                "请使用卷宗录入 POST /case-intake/process 或文书识别 POST /ai/documents/recognize；"
                        + "大文件分片请用 POST /upload/init");
    }

    /**
     * 初始化分片上传
     * POST /api/upload/init
     */
    @PostMapping("/init")
    @PreAuthorize("isAuthenticated()")
    public Result<String> initChunkedUpload(
            @RequestParam String fileName,
            @RequestParam long fileSize,
            @RequestParam String mimeType) {
        try {
            String uploadId = chunkedUploadService.initChunkedUpload(fileName, fileSize, mimeType);
            return Result.success(uploadId);
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            return Result.error("初始化分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传分片
     * POST /api/upload/chunk
     */
    @PostMapping("/chunk")
    @PreAuthorize("isAuthenticated()")
    public Result<ChunkedUploadService.UploadProgress> uploadChunk(
            @RequestParam String uploadId,
            @RequestParam int chunkIndex,
            @RequestParam MultipartFile chunk) {
        try {
            ChunkedUploadService.UploadProgress progress = chunkedUploadService.uploadChunk(uploadId, chunkIndex, chunk);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("上传分片失败: uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return Result.error("上传分片失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片
     * POST /api/upload/merge
     */
    @PostMapping("/merge")
    @PreAuthorize("isAuthenticated()")
    public Result<String> mergeChunks(@RequestParam String uploadId) {
        try {
            String finalPath = chunkedUploadService.mergeChunks(uploadId);
            return Result.success(finalPath);
        } catch (Exception e) {
            log.error("合并分片失败: uploadId={}", uploadId, e);
            return Result.error("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传进度
     * GET /api/upload/progress/{uploadId}
     */
    @GetMapping("/progress/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    public Result<ChunkedUploadService.UploadProgress> getUploadProgress(@PathVariable String uploadId) {
        try {
            ChunkedUploadService.UploadProgress progress = chunkedUploadService.getUploadProgress(uploadId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取上传进度失败: uploadId={}", uploadId, e);
            return Result.error("获取上传进度失败: " + e.getMessage());
        }
    }

    /**
     * 取消上传
     * DELETE /api/upload/{uploadId}
     */
    @DeleteMapping("/{uploadId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> cancelUpload(@PathVariable String uploadId) {
        try {
            chunkedUploadService.cancelUpload(uploadId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消上传失败: uploadId={}", uploadId, e);
            return Result.error("取消上传失败: " + e.getMessage());
        }
    }
}