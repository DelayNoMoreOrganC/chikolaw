package com.lawfirm.service;

import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.CaseFileIntakeResult;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CaseFileIntakeServiceTest {

    @Mock
    private CaseDocumentService caseDocumentService;
    @Mock
    private CaseTimelineService caseTimelineService;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private EmbeddedAgentService embeddedAgentService;
    @Mock
    private CaseIntakePendingService caseIntakePendingService;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private CaseFileIntakeService caseFileIntakeService;

    private MockMultipartFile sampleFile;

    @BeforeEach
    void setUp() {
        sampleFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf",
                "dummy".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void processIntake_withoutCaseMatch_returnsNeedsCaseAndPendingId() throws Exception {
        AIDocumentRecognitionResult recognition = new AIDocumentRecognitionResult();
        recognition.setCaseNumber("2026-TEST-001");
        recognition.setDocumentType("起诉状");

        EmbeddedAgentService.AnalysisResult analysis =
                new EmbeddedAgentService.AnalysisResult(recognition, "builtin");

        when(embeddedAgentService.analyzeDocument(any(), eq(1L), isNull(), isNull()))
                .thenReturn(analysis);
        when(caseRepository.findByCaseNumberAndDeletedFalse("2026-TEST-001"))
                .thenReturn(Optional.empty());
        when(caseIntakePendingService.savePending(any(), eq(1L), isNull(), eq(recognition)))
                .thenReturn(99L);

        CaseFileIntakeResult result = caseFileIntakeService.processIntake(
                sampleFile, 1L, null, null, false);

        assertEquals("NEEDS_CASE", result.getStatus());
        assertEquals(99L, result.getPendingId());
        assertNotNull(result.getRecognition());
        verify(caseRepository, never()).findById(anyLong());
    }

    @Test
    void processIntake_withExplicitCaseId_doesNotCreateCase() throws Exception {
        AIDocumentRecognitionResult recognition = new AIDocumentRecognitionResult();
        recognition.setDocumentType("答辩状");
        EmbeddedAgentService.AnalysisResult analysis =
                new EmbeddedAgentService.AnalysisResult(recognition, "builtin");

        Case caseEntity = new Case();
        caseEntity.setId(5L);
        caseEntity.setCaseName("测试案");
        caseEntity.setCaseNumber("2026-005");

        com.lawfirm.dto.CaseDocumentDTO docDto = new com.lawfirm.dto.CaseDocumentDTO();
        docDto.setId(10L);
        docDto.setDocumentName("test.pdf");

        when(embeddedAgentService.analyzeDocument(any(), eq(1L), eq(5L), any()))
                .thenReturn(analysis);
        when(caseRepository.findById(5L)).thenReturn(Optional.of(caseEntity));
        when(caseDocumentService.resolveStageDocumentFolder(eq(caseEntity), eq("答辩状")))
                .thenReturn("答辩状");
        doReturn(docDto).when(caseDocumentService)
                .uploadDocument(eq(5L), any(), eq("答辩状"), eq("答辩状"), eq(1L));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(caseDocumentService.updateDocument(anyLong(), any())).thenReturn(docDto);
        when(caseTimelineService.createSystemTimeline(eq(5L), eq("FILE_INTAKE"), anyString()))
                .thenReturn(new com.lawfirm.entity.CaseTimeline());

        CaseFileIntakeResult result = caseFileIntakeService.processIntake(
                sampleFile, 1L, 5L, "备注", false);

        assertEquals("SUCCESS", result.getStatus(), () -> "msg=" + result.getMessage());
        assertEquals(5L, result.getCaseId());
        verify(caseIntakePendingService, never()).savePending(any(), anyLong(), any(), any());
    }

    @Test
    void processIntake_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class,
                () -> caseFileIntakeService.processIntake(empty, 1L, null, null, false));
    }
}
