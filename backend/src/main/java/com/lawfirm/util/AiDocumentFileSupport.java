package com.lawfirm.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文书 AI 识别支持的文件类型（与卷宗录入对齐）。
 */
public final class AiDocumentFileSupport {

    private static final long MAX_RECOGNIZE_BYTES = 50L * 1024 * 1024;

    private AiDocumentFileSupport() {
    }

    public static boolean isSupported(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf")
                || lower.endsWith(".docx")
                || lower.endsWith(".txt")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png");
    }

    public static String unsupportedMessage() {
        return "支持 PDF、Word(docx)、TXT、JPG、PNG 格式";
    }

    public static boolean exceedsMaxSize(MultipartFile file) {
        return file != null && file.getSize() > MAX_RECOGNIZE_BYTES;
    }

    public static String maxSizeMessage() {
        return "文件大小不能超过 50MB";
    }
}
