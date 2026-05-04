package com.lawfirm.service;

import com.lawfirm.dto.CaseCreateRequest;
import com.lawfirm.dto.CaseImportResultDTO;
import com.lawfirm.dto.PartyDTO;
import com.lawfirm.vo.CaseDetailVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseImportService {

    private final CaseService caseService;

    @Transactional
    public CaseImportResultDTO importNpaCases(MultipartFile file, Long currentUserId) {
        CaseImportResultDTO result = new CaseImportResultDTO();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            result.setTotalRows(Math.max(sheet.getLastRowNum(), 0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row)) {
                    continue;
                }

                int rowNumber = i + 1;
                try {
                    CaseCreateRequest request = toRequest(row, currentUserId);
                    CaseDetailVO created = caseService.createCase(request, currentUserId);
                    result.addSuccess(rowNumber, created.getId(), created.getCaseNumber());
                } catch (Exception e) {
                    result.addFailure(rowNumber, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }

        return result;
    }

    private CaseCreateRequest toRequest(Row row, Long currentUserId) {
        CaseCreateRequest request = new CaseCreateRequest();
        String bankName = text(row, 0);
        String debtorName = text(row, 1);
        String batchNo = text(row, 2);

        if (bankName.isBlank()) {
            throw new IllegalArgumentException("委托银行不能为空");
        }
        if (debtorName.isBlank()) {
            throw new IllegalArgumentException("债务人/被告不能为空");
        }

        request.setCaseType("FINANCIAL_NPA");
        request.setProcedure(defaultValue(text(row, 11), "FIRST_INSTANCE"));
        request.setLevel("GENERAL");
        request.setOwnerId(currentUserId);
        request.setEntrustingBankName(bankName);
        request.setAssetBatchNo(batchNo);
        request.setNpaSubtype(text(row, 3));
        request.setCaseNumber(text(row, 4));
        request.setLoanContractNo(text(row, 5));
        request.setTransferAgreementNo(text(row, 6));
        request.setPrincipalBalance(decimal(row, 7));
        request.setInterestBalance(decimal(row, 8));
        request.setGuaranteeType(text(row, 9));
        request.setCollateralStatus(text(row, 10));
        request.setCaseReason(defaultValue(text(row, 12), "金融借款合同纠纷"));
        request.setCaseName(bankName + "诉" + debtorName + "金融不良资产清收案");
        request.setAmount(sum(request.getPrincipalBalance(), request.getInterestBalance()));

        PartyDTO plaintiff = new PartyDTO();
        plaintiff.setPartyType("ORGANIZATION");
        plaintiff.setPartyRole("PLAINTIFF");
        plaintiff.setName(bankName);
        plaintiff.setIsClient(true);
        plaintiff.setSyncToClient(true);

        PartyDTO defendant = new PartyDTO();
        defendant.setPartyType("INDIVIDUAL");
        defendant.setPartyRole("DEFENDANT");
        defendant.setName(debtorName);
        defendant.setPhone(text(row, 13));
        defendant.setIdCard(text(row, 14));
        defendant.setAddress(text(row, 15));

        List<PartyDTO> parties = new ArrayList<>();
        parties.add(plaintiff);
        parties.add(defendant);
        request.setParties(parties);
        return request;
    }

    private boolean isBlank(Row row) {
        for (int i = 0; i < 16; i++) {
            if (!text(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String text(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
    }

    private BigDecimal decimal(Row row, int cellIndex) {
        String value = text(row, cellIndex);
        if (value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.replace(",", ""));
    }

    private BigDecimal sum(BigDecimal left, BigDecimal right) {
        BigDecimal result = BigDecimal.ZERO;
        if (left != null) result = result.add(left);
        if (right != null) result = result.add(right);
        return result;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
