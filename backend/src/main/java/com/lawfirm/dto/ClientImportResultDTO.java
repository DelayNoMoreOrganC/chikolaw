package com.lawfirm.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClientImportResultDTO {
    private int totalRows;
    private int successRows;
    private int failedRows;
    private int conflictRows;
    private List<RowResult> rows = new ArrayList<>();

    public void addSuccess(int rowNumber, Long clientId, String clientName) {
        RowResult row = new RowResult();
        row.setRowNumber(rowNumber);
        row.setSuccess(true);
        row.setClientId(clientId);
        row.setClientName(clientName);
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

    public void addConflict(int rowNumber, String clientName, String conflictDetail) {
        RowResult row = new RowResult();
        row.setRowNumber(rowNumber);
        row.setSuccess(false);
        row.setConflict(true);
        row.setClientName(clientName);
        row.setError(conflictDetail);
        rows.add(row);
        conflictRows++;
        failedRows++;
    }

    @Data
    public static class RowResult {
        private int rowNumber;
        private boolean success;
        private boolean conflict;
        private Long clientId;
        private String clientName;
        private String error;
    }
}
