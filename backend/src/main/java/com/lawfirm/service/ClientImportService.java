package com.lawfirm.service;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.dto.ClientImportResultDTO;
import com.lawfirm.dto.ConflictCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 客户库 Excel 批量导入（行政要求：历史客户导入 + 利冲审查）
 * 模板列：客户名称 | 客户类型 | 电话 | 身份证号 | 统一社会信用代码 | 邮箱 | 地址 | 备注
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientImportService {

    private final ClientService clientService;
    private final ConflictCheckService conflictCheckService;

    @Transactional
    public ClientImportResultDTO importClients(MultipartFile file, Long userId, boolean skipConflictRows) {
        ClientImportResultDTO result = new ClientImportResultDTO();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            result.setTotalRows(Math.max(sheet.getLastRowNum(), 0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int rowNumber = i + 1;
                try {
                    ClientDTO dto = toDto(row, userId);
                    ConflictCheckResult conflict = conflictCheckService.checkClientNameConflict(dto.getClientName());
                    if (Boolean.TRUE.equals(conflict.getHasConflict())) {
                        String detail = conflict.getConflicts() != null && !conflict.getConflicts().isEmpty()
                                ? conflict.getConflicts().get(0).getDescription()
                                : "存在利益冲突或高度相似客户";
                        if (skipConflictRows) {
                            result.addConflict(rowNumber, dto.getClientName(), detail);
                            continue;
                        }
                        throw new IllegalArgumentException("利冲未通过: " + detail);
                    }
                    ClientDTO created = clientService.createClient(dto, userId);
                    result.addSuccess(rowNumber, created.getId(), created.getClientName());
                } catch (Exception e) {
                    result.addFailure(rowNumber, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("客户导入失败: " + e.getMessage(), e);
        }

        log.info("客户导入完成: 成功={}, 失败={}, 利冲跳过={}",
                result.getSuccessRows(), result.getFailedRows(), result.getConflictRows());
        return result;
    }

    private ClientDTO toDto(Row row, Long userId) {
        String clientName = text(row, 0);
        if (clientName.isBlank()) {
            throw new IllegalArgumentException("客户名称不能为空");
        }
        ClientDTO dto = new ClientDTO();
        dto.setClientName(clientName.trim());
        dto.setClientType(normalizeClientType(text(row, 1)));
        dto.setPhone(text(row, 2));
        dto.setIdCard(text(row, 3));
        dto.setCreditCode(text(row, 4));
        dto.setEmail(text(row, 5));
        dto.setAddress(text(row, 6));
        dto.setNotes(text(row, 7));
        dto.setOwnerId(userId);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private String normalizeClientType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "INDIVIDUAL";
        }
        String t = raw.trim();
        if (t.contains("企业") || t.contains("公司") || "ORGANIZATION".equalsIgnoreCase(t)) {
            return "ORGANIZATION";
        }
        if (t.contains("机关") || t.contains("事业")) {
            return "INSTITUTION";
        }
        return "INDIVIDUAL";
    }

    private boolean isBlankRow(Row row) {
        return text(row, 0).isBlank();
    }

    private String text(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
