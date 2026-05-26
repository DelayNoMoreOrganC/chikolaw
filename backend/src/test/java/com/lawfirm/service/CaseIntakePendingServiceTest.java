package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.CaseIntakePrefillDTO;
import com.lawfirm.entity.CaseIntakePending;
import com.lawfirm.repository.CaseIntakePendingRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
