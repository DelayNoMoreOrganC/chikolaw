package com.lawfirm.enums;

/**
 * AI 模型按业务场景路由（首选 provider，失败时由 {@link com.lawfirm.service.LLMApiService} 统一降级）。
 */
public enum AIModelUseCase {
    /** 法律咨询（LegalChatService） */
    LEGAL_CHAT,
    /** 知识库 RAG（RAGService） */
    RAG,
    /** 文书生成（DocumentGenerationService） */
    DOCUMENT,
    /** 通用/案件对话（AiChatService） */
    GENERAL_CHAT,
    /** OCR 后要素提取等（LlmExtractService） */
    EXTRACT,
    /** 文档识别后的结构化抽取（AIDocumentService.extractLegalInfo） */
    DOCUMENT_RECOGNITION_EXTRACT,
    /** 兼容旧版文书生成（DocGenerateService） */
    LEGACY_DOCUMENT
}
