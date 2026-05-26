package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.CaseFileIntakeResult;
import com.lawfirm.dto.CaseIntakeDraftResultDTO;
import com.lawfirm.dto.CaseIntakePrefillDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseIntakePending;
import com.lawfirm.repository.CaseIntakePendingRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import com.lawfirm.vo.CaseDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseIntakePendingServiceTest {

    @Mock
    private CaseIntakePendingRepository pendingRepository;
    @Mock
    private CaseFileIntakeService caseFileIntakeService;
    @Mock
    private ApprovalService approvalService;
    @Mock
    private CaseService caseService;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CaseIntakePendingService caseIntakePendingService;

    @Test
    void getPrefill_mapsRecognitionFields() throws Exception {
        AIDocumentRecognitionResult rec = new AIDocumentRecognitionResult();
        rec.setCaseNumber("2026-001");
        rec.setPlaintiffName("张三");
        rec.setDefendantName("李四");
        rec.setCaseReason("买卖合同纠纷");

        CaseIntakePending pending = new CaseIntakePending();
        pending.setId(7L);
        pending.setUserId(1L);
        pending.setStatus("FILING_APPROVED");
        pending.setRecognitionJson(objectMapper.writeValueAsString(rec));

        when(pendingRepository.findByIdAndUserIdAndDeletedFalse(7L, 1L))
                .thenReturn(Optional.of(pending));

        CaseIntakePrefillDTO dto = caseIntakePendingService.getPrefill(7L, 1L);

        assertEquals(7L, dto.getPendingId());
        assertEquals("2026-001", dto.getCaseNumber());
        assertEquals("张三诉李四买卖合同纠纷案", dto.getSuggestedCaseName());
    }

    @Test
    void getPrefill_includesDraftCaseWhenLinked() {
        CaseIntakePending pending = new CaseIntakePending();
        pending.setId(8L);
        pending.setUserId(1L);
        pending.setStatus("DRAFT_CREATED");
        pending.setCaseId(100L);

        Case caseEntity = new Case();
        caseEntity.setId(100L);
        caseEntity.setCaseNumber("CASE-100");
        caseEntity.setCaseName("草稿案件");

        when(pendingRepository.findByIdAndUserIdAndDeletedFalse(8L, 1L))
                .thenReturn(Optional.of(pending));
        when(caseRepository.findById(100L)).thenReturn(Optional.of(caseEntity));

        CaseIntakePrefillDTO dto = caseIntakePendingService.getPrefill(8L, 1L);

        assertEquals(100L, dto.getDraftCaseId());
        assertEquals("CASE-100", dto.getDraftCaseNumber());
    }

    @Test
    void createDraftCaseAfterFilingApproved_createsAndAttaches(@TempDir Path tempDir) throws Exception {
        AIDocumentRecognitionResult rec = new AIDocumentRecognitionResult();
        rec.setPlaintiffName("张三");
        rec.setDefendantName("李四");

        Path stored = tempDir.resolve("doc.pdf");
        Files.writeString(stored, "test");

        CaseIntakePending pending = new CaseIntakePending();
        pending.setId(9L);
        pending.setUserId(2L);
        pending.setStatus("FILING_APPROVED");
        pending.setRecognitionJson(objectMapper.writeValueAsString(rec));
        pending.setRemark("测试");
        pending.setStoredPath(stored.toString());
        pending.setOriginalFilename("doc.pdf");
        pending.setFileSize(4L);

        CaseDetailVO created = new CaseDetailVO();
        created.setId(200L);
        created.setCaseNumber("CASE-200");
        created.setCaseName("张三诉李四案");

        when(pendingRepository.findById(9L)).thenReturn(Optional.of(pending));
        when(pendingRepository.findByIdAndUserIdAndDeletedFalse(9L, 2L)).thenReturn(Optional.of(pending));
        when(caseService.createCase(any(), eq(2L))).thenReturn(created);

        CaseFileIntakeResult attachResult = new CaseFileIntakeResult();
        attachResult.setStatus("SUCCESS");
        when(caseFileIntakeService.attachToCase(any(), eq(2L), eq(200L), anyString(), any()))
                .thenReturn(attachResult);

        CaseIntakeDraftResultDTO result =
                caseIntakePendingService.createDraftCaseAfterFilingApproved(9L, 2L);

        assertEquals(200L, result.getDraftCaseId());
        assertTrue(result.isIntakeAttached());
        verify(caseService).createCase(any(), eq(2L));
    }
}
