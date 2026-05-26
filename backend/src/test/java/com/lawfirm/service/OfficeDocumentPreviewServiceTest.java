package com.lawfirm.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OfficeDocumentPreviewServiceTest {

    private OfficeDocumentPreviewService service;

    @BeforeEach
    void setUp() {
        service = new OfficeDocumentPreviewService();
        ReflectionTestUtils.setField(service, "maxBytes", 10 * 1024 * 1024L);
    }

    @Test
    void supports_officeExtensions() {
        assertTrue(service.supports("合同.docx"));
        assertTrue(service.supports("清单.xlsx"));
        assertFalse(service.supports("scan.pdf"));
    }

    @Test
    void convertDocx_includesParagraphText() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph para = doc.createParagraph();
            para.createRun().setText("局域网预览测试");
            doc.write(baos);
        }
        String html = service.convertToHtml(
                new ByteArrayInputStream(baos.toByteArray()), "test.docx", baos.size());
        assertTrue(html.contains("局域网预览测试"));
        assertTrue(html.contains("<!DOCTYPE html>"));
    }

    @Test
    void convertXlsx_rendersTable() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("案号");
            row.createCell(1).setCellValue("2026-001");
            workbook.write(baos);
        }
        String html = service.convertToHtml(
                new ByteArrayInputStream(baos.toByteArray()), "data.xlsx", baos.size());
        assertTrue(html.contains("案号"));
        assertTrue(html.contains("2026-001"));
        assertTrue(html.contains("office-table"));
    }

    @Test
    void convertToHtml_rejectsOversizedFile() {
        assertThrows(IllegalArgumentException.class, () ->
                service.convertToHtml(new ByteArrayInputStream(new byte[0]), "big.docx", 20 * 1024 * 1024L));
    }
}
