package com.lawfirm.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 局域网内 Office 文档预览：服务端转为 HTML（无需外网 Office Online）。
 */
@Slf4j
@Service
public class OfficeDocumentPreviewService {

    private static final Set<String> SUPPORTED = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    private static final int MAX_EXCEL_ROWS = 500;
    private static final int MAX_EXCEL_COLS = 40;

    @Value("${document.preview.max-bytes:15728640}")
    private long maxBytes;

    public boolean supports(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return SUPPORTED.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public String convertToHtml(InputStream input, String filename, long fileSize) throws IOException {
        if (fileSize > maxBytes) {
            throw new IllegalArgumentException("文件过大，超过预览大小限制（"
                    + (maxBytes / 1024 / 1024) + "MB），请下载后查看");
        }
        String ext = extension(filename);
        switch (ext) {
            case "docx":
                return wrapHtml(convertDocx(input));
            case "doc":
                return wrapHtml(convertDoc(input));
            case "xls":
            case "xlsx":
                return wrapHtml(convertExcel(input));
            case "ppt":
                return wrapHtml(convertPpt(input));
            case "pptx":
                return wrapHtml(convertPptx(input));
            default:
                throw new IllegalArgumentException("不支持的 Office 预览格式: " + ext);
        }
    }

    private String convertDocx(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(input)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    body.append("<p>").append(escape(text.trim())).append("</p>\n");
                }
            }
            for (XWPFTable table : doc.getTables()) {
                body.append("<table class=\"office-table\">\n");
                for (XWPFTableRow row : table.getRows()) {
                    body.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        body.append("<td>").append(escape(cell.getText().trim())).append("</td>");
                    }
                    body.append("</tr>\n");
                }
                body.append("</table>\n");
            }
        }
        if (body.length() == 0) {
            body.append("<p class=\"muted\">（文档无文本内容）</p>");
        }
        return body.toString();
    }

    private String convertDoc(InputStream input) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(input);
             WordExtractor extractor = new WordExtractor(doc)) {
            String text = extractor.getText();
            if (text == null || text.isBlank()) {
                return "<p class=\"muted\">（文档无文本内容）</p>";
            }
            StringBuilder body = new StringBuilder();
            for (String line : text.split("\\R")) {
                if (!line.isBlank()) {
                    body.append("<p>").append(escape(line.trim())).append("</p>\n");
                }
            }
            return body.toString();
        }
    }

    private String convertExcel(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(input)) {
            int sheetCount = workbook.getNumberOfSheets();
            for (int s = 0; s < sheetCount; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet == null) {
                    continue;
                }
                if (sheetCount > 1) {
                    body.append("<h3>").append(escape(sheet.getSheetName())).append("</h3>\n");
                }
                body.append("<table class=\"office-table\">\n");
                int lastRow = Math.min(sheet.getLastRowNum(), MAX_EXCEL_ROWS - 1);
                for (int r = 0; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    body.append("<tr>");
                    int lastCell = Math.min(
                            row.getLastCellNum() > 0 ? row.getLastCellNum() - 1 : 0,
                            MAX_EXCEL_COLS - 1);
                    for (int c = 0; c <= lastCell; c++) {
                        Cell cell = row.getCell(c);
                        body.append("<td>").append(escape(cellText(cell))).append("</td>");
                    }
                    body.append("</tr>\n");
                }
                body.append("</table>\n");
                if (sheet.getLastRowNum() >= MAX_EXCEL_ROWS) {
                    body.append("<p class=\"muted\">（仅显示前 ").append(MAX_EXCEL_ROWS).append(" 行）</p>\n");
                }
            }
        }
        if (body.length() == 0) {
            body.append("<p class=\"muted\">（表格无数据）</p>");
        }
        return body.toString();
    }

    private String convertPpt(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (HSLFSlideShow ppt = new HSLFSlideShow(input)) {
            int idx = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                body.append("<section class=\"slide\"><h4>幻灯片 ").append(idx++).append("</h4>");
                String title = slide.getTitle();
                if (title != null && !title.isBlank()) {
                    body.append("<p><strong>").append(escape(title.trim())).append("</strong></p>");
                }
                for (org.apache.poi.hslf.usermodel.HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        String text = ((HSLFTextShape) shape).getText();
                        if (text != null && !text.isBlank()) {
                            body.append("<p>").append(escape(text.trim())).append("</p>");
                        }
                    }
                }
                body.append("</section>\n");
            }
        }
        if (body.length() == 0) {
            body.append("<p class=\"muted\">（演示文稿无文本内容）</p>");
        }
        return body.toString();
    }

    private String convertPptx(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (XMLSlideShow ppt = new XMLSlideShow(input)) {
            int idx = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                body.append("<section class=\"slide\"><h4>幻灯片 ").append(idx++).append("</h4>");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        String text = ((XSLFTextShape) shape).getText();
                        if (text != null && !text.isBlank()) {
                            body.append("<p>").append(escape(text.trim())).append("</p>");
                        }
                    }
                }
                body.append("</section>\n");
            }
        }
        if (body.length() == 0) {
            body.append("<p class=\"muted\">（演示文稿无文本内容）</p>");
        }
        return body.toString();
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String wrapHtml(String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>"
                + "<style>"
                + "body{font-family:\"Microsoft YaHei\",SimSun,sans-serif;font-size:14px;line-height:1.6;"
                + "margin:12px;color:#333;}"
                + ".office-table{border-collapse:collapse;width:100%;margin:8px 0;}"
                + ".office-table td,.office-table th{border:1px solid #dcdfe6;padding:6px 8px;}"
                + ".slide{margin-bottom:16px;padding-bottom:12px;border-bottom:1px dashed #e4e7ed;}"
                + ".muted{color:#909399;}"
                + "h3,h4{margin:12px 0 8px;}"
                + "</style></head><body>"
                + body
                + "</body></html>";
    }
}
