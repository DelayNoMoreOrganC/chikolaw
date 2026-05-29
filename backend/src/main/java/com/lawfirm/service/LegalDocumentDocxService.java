package com.lawfirm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 将纯文本法律文书导出为 Word (.docx)。
 */
@Slf4j
@Service
public class LegalDocumentDocxService {

    public byte[] textToDocx(String title, String content) {
        if (content == null) {
            content = "";
        }
        String docTitle = title != null && !title.isBlank() ? title.trim() : "法律文书";

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(docTitle);
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setFontFamily("宋体");

            document.createParagraph();

            for (String line : content.split("\\r?\\n")) {
                XWPFParagraph para = document.createParagraph();
                XWPFRun run = para.createRun();
                run.setText(line);
                run.setFontSize(12);
                run.setFontFamily("宋体");
            }

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成 docx 失败", e);
            throw new IllegalStateException("生成 Word 文档失败: " + e.getMessage(), e);
        }
    }
}
