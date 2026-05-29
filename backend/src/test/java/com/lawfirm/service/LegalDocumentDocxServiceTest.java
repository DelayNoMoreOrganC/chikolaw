package com.lawfirm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegalDocumentDocxServiceTest {

  private final LegalDocumentDocxService service = new LegalDocumentDocxService();

  @Test
  void textToDocx_producesNonEmptyBytes() {
    byte[] docx = service.textToDocx("起诉状", "原告：张三\n被告：李四");
    assertNotNull(docx);
    assertTrue(docx.length > 100);
    assertEquals('P', (char) docx[0]);
    assertEquals('K', (char) docx[1]);
  }
}
