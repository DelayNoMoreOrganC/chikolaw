package com.lawfirm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTypeAliasResolverTest {

    @Test
    void normalize_standardAndAliases() {
        assertEquals("COMPLAINT", DocumentTypeAliasResolver.normalize("complaint"));
        assertEquals("COMPLAINT", DocumentTypeAliasResolver.normalize("起诉状"));
        assertEquals("DEFENSE_STATEMENT", DocumentTypeAliasResolver.normalize("defense"));
        assertEquals("BRIEF", DocumentTypeAliasResolver.normalize("opinion"));
        assertEquals("BRIEF", DocumentTypeAliasResolver.normalize("legalBrief"));
        assertEquals("LEGAL_OPINION", DocumentTypeAliasResolver.normalize("legal_opinion"));
        assertEquals("LAWYER_LETTER", DocumentTypeAliasResolver.normalize("律师函"));
        assertEquals("LAWYER_LETTER", DocumentTypeAliasResolver.normalize("letter"));
    }

    @Test
    void displayName_returnsChineseLabel() {
        assertEquals("起诉状", DocumentTypeAliasResolver.displayName("complaint"));
        assertEquals("律师函", DocumentTypeAliasResolver.displayName("LAWYER_LETTER"));
    }

    @Test
    void isLegacyDocumentType_onlyLawyerLetter() {
        assertTrue(DocumentTypeAliasResolver.isLegacyDocumentType("律师函"));
        assertFalse(DocumentTypeAliasResolver.isLegacyDocumentType("COMPLAINT"));
    }
}
