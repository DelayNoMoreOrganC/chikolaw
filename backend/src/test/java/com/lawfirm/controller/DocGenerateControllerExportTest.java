package com.lawfirm.controller;

import com.lawfirm.dto.TextToDocxRequest;
import com.lawfirm.service.DocGenerateService;
import com.lawfirm.service.LegalDocumentDocxService;
import com.lawfirm.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DocGenerateControllerExportTest {

    @Mock
    private DocGenerateService docGenerateService;
    @Mock
    private SecurityUtil securityUtil;

    private DocGenerateController controller;

    @BeforeEach
    void setUp() {
        controller = new DocGenerateController(
                docGenerateService,
                new LegalDocumentDocxService(),
                securityUtil);
    }

    @Test
    void exportDocx_returnsDocxAttachment() {
        TextToDocxRequest req = new TextToDocxRequest();
        req.setTitle("起诉状");
        req.setContent("原告：张三\n被告：李四");
        req.setFileName("test_export");

        ResponseEntity<byte[]> response = controller.exportDocx(req);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 100);
        assertEquals('P', (char) response.getBody()[0]);
        assertEquals('K', (char) response.getBody()[1]);
        assertNotNull(response.getHeaders().getContentDisposition());
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("docx"));
    }
}
