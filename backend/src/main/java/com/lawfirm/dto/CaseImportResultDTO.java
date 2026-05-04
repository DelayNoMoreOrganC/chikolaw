package com.lawfirm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CaseImportResultDTO {
    private int totalRows;
    private int successRows;
    private int failedRows;
    private List<RowResult> rows = new ArrayList<>();

    public void addSuccess(int rowNumber, Long caseId, String caseNumber) {
        RowResult row = new RowResult();
        row.setRowNumber(rowNumber);
        row.setSuccess(true);
        row.setCaseId(caseId);
        row.setCaseNumber(caseNumber);
        rows.add(row);
        successRows++;
    }

    public void addFailure(int rowNumber, String error) {
        RowResult row = new RowResult();
        row.setRowNumber(rowNumber);
        row.setSuccess(false);
        row.setError(error);
        rows.add(row);
        failedRows++;
    }

    @Data
    public static class RowResult {
        private int rowNumber;
        private boolean success;
        private Long caseId;
        private String caseNumber;
        private String error;
    }
}
