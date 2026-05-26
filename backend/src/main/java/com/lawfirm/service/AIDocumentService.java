package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIFunctionType;
import com.lawfirm.enums.AIModelUseCase;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * AI文档智能识别服务
 * 实现OCR识别 + LLM要素提取的核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDocumentService {

    private final AIModelRoutingService aimodelRoutingService;
    private final AILogService aiLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBusinessLogicHandler businessLogicHandler;
    private final LLMApiService llmApiService;

    @Value("${ai.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${ai.ocr.provider:tesseract}")
    private String ocrProvider;

    @Value("${ai.ocr.pdf-vision-max-pages:5}")
    private int pdfVisionMaxPages;

    @Value("${ai.ocr.pdf-vision-dpi:120}")
    private int pdfVisionDpi;

    /**
     * 智能识别法院文书（默认执行业务侧效：待办/建案等）。
     */
    public AIDocumentRecognitionResult recognizeLegalDocument(MultipartFile file, Long userId, Long caseId) {
        return recognizeLegalDocument(file, userId, caseId, true);
    }

    /**
     * 智能识别法院文书。
     *
     * @param executeBusinessLogic false 时仅 OCR+要素提取（卷宗录入由 {@link CaseFileIntakeService} 统一归档后再决定是否自动化）
     */
    public AIDocumentRecognitionResult recognizeLegalDocument(MultipartFile file, Long userId, Long caseId,
                                                            boolean executeBusinessLogic) {
        long startTime = System.currentTimeMillis();
        AIConfig aiConfig = null;
        String modelName = "";

        try {
            try {
                aiConfig = aimodelRoutingService.resolveForUseCase(AIModelUseCase.DOCUMENT_RECOGNITION_EXTRACT);
            } catch (Exception e) {
                log.warn("文档识别抽取场景无可用 AI 配置，使用默认 Ollama 占位: {}", e.getMessage());
                aiConfig = createDefaultOllamaConfig();
            }
            modelName = aiConfig.getModelName() != null ? aiConfig.getModelName() : aiConfig.getProviderType();

            String ocrText = performOCR(file);
            log.info("OCR识别完成，文本长度: {}", ocrText.length());

            AIDocumentRecognitionResult result = extractLegalInfo(ocrText, aiConfig);
            result.setOcrText(ocrText);

            if (executeBusinessLogic
                    && result.getDocumentType() != null && !result.getDocumentType().isEmpty()) {
                executeBusinessLogic(result, userId);
            }

            result.setProcessingTime(System.currentTimeMillis() - startTime);

            int duration = (int) (System.currentTimeMillis() - startTime);
            String inputBrief = "file=" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "")
                    + " size=" + file.getSize() + " ocrLen=" + ocrText.length();
            String outBrief = result.getDocumentType() != null
                    ? "docType=" + result.getDocumentType()
                    : "extracted";
            aiLogService.log(userId, caseId, AIFunctionType.OCR_RECOGNITION,
                    inputBrief, null, outBrief, null, modelName, "SUCCESS", duration, null);

            return result;

        } catch (Exception e) {
            log.error("文档识别失败", e);
            int duration = (int) (System.currentTimeMillis() - startTime);
            String inputBrief = "file=" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "")
                    + " size=" + file.getSize();
            aiLogService.log(userId, caseId, AIFunctionType.OCR_RECOGNITION,
                    inputBrief, null, null, null, modelName, "FAILED", duration, e.getMessage());

            throw new AIServiceException("文档识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行OCR识别
     */
    private String performOCR(MultipartFile file) throws Exception {
        if (!ocrEnabled) {
            throw new AIServiceException("OCR功能未启用");
        }

        switch (ocrProvider.toLowerCase()) {
            case "tesseract":
                return performTesseractOCR(file);
            case "deepseek":
                return performDeepSeekVisionOCR(file);
            case "baidu":
                return performBaiduOCR(file);
            case "aliyun":
                return performAliyunOCR(file);
            default:
                throw new AIServiceException("不支持的OCR提供商: " + ocrProvider);
        }
    }

    /**
     * Tesseract OCR（本地部署）
     */
    private String performTesseractOCR(MultipartFile file) throws Exception {
        log.info("开始PDF文档OCR识别，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());

        try {
            // 1. 保存临时文件
            java.io.File tempFile = java.io.File.createTempFile("ocr_", ".pdf");
            file.transferTo(tempFile);
            log.debug("临时文件已保存: {}", tempFile.getAbsolutePath());

            // 2. 优先使用PDFBox提取PDF文本（如果PDF有文本层）
            String extractedText = extractTextFromPDF(tempFile);

            // 3. 如果提取的文本太少，尝试使用Tesseract OCR
            if (extractedText == null || extractedText.trim().length() < 100) {
                log.warn("PDF文本提取失败或文本太少(长度: {})，这是扫描版PDF，需要Tesseract OCR", extractedText == null ? 0 : extractedText.length());
                extractedText = "该PDF文档似乎是扫描版，当前OCR功能未完全配置。建议：1. 使用有文本层的PDF 2. 或手动输入判决书信息";
            } else {
                log.info("PDF文本提取成功，文本长度: {}", extractedText.length());
            }

            // 4. 删除临时文件
            tempFile.delete();
            log.debug("临时文件已删除");

            return extractedText;

        } catch (Exception e) {
            log.error("PDF文档处理失败", e);
            throw new AIServiceException("PDF文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从PDF中提取文本（使用Apache PDFBox）
     */
    private String extractTextFromPDF(java.io.File pdfFile) throws Exception {
        log.debug("开始使用PDFBox提取文本");

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile)) {
            StringBuilder text = new StringBuilder();

            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                text.append(pageText).append("\n");
            }

            String result = text.toString().trim();
            log.debug("PDFBox提取完成，页数: {}, 文本长度: {}", document.getNumberOfPages(), result.length());
            return result;
        }
    }

    /**
     * 百度OCR API
     */
    private String performBaiduOCR(MultipartFile file) throws Exception {
        // TODO: 集成百度OCR API
        log.warn("百度OCR尚未实现");
        return "";
    }

    /**
     * 阿里云OCR API
     */
    private String performAliyunOCR(MultipartFile file) throws Exception {
        // TODO: 集成阿里云OCR API
        log.warn("阿里云OCR尚未实现");
        return "";
    }

    /**
     * DeepSeek Vision API（图片识别）
     * 使用DeepSeek视觉模型进行OCR识别
     */
    private String performDeepSeekVisionOCR(MultipartFile file) throws Exception {
        log.info("开始使用DeepSeek Vision API进行OCR识别，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());

        try {
            String contentType = file.getContentType();
            boolean isPdf = "application/pdf".equals(contentType)
                    || (file.getOriginalFilename() != null
                    && file.getOriginalFilename().toLowerCase().endsWith(".pdf"));

            if (!isPdf && (contentType == null || !contentType.startsWith("image/"))) {
                throw new AIServiceException("不支持的文件类型，仅支持图片和PDF");
            }

            if (isPdf) {
                String pdfText = extractTextFromPDF(file);
                if (pdfText != null && pdfText.trim().length() > 100) {
                    log.info("PDF文本层提取成功，文本长度: {}", pdfText.length());
                    return pdfText;
                }
                log.warn("PDF 文本层不足（长度 {}），分页渲染后调用 Vision",
                        pdfText == null ? 0 : pdfText.trim().length());
                return visionOcrPdfPages(file);
            }

            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.debug("图片已转换为Base64，长度: {}", base64Image.length());

            // 4. 构建OCR提示词
            String ocrPrompt = "请仔细识别图片中的所有文字内容，包括中文、英文、数字、标点符号等。" +
                    "如果是法律文书，请特别注意：\n" +
                    "1. 案号的准确性\n" +
                    "2. 当事人姓名、公司名称\n" +
                    "3. 日期格式\n" +
                    "4. 金额数字\n" +
                    "5. 法律条文引用\n\n" +
                    "请按照原文的格式和排版输出识别结果，保持段落结构和换行。";

            // 5. 调用DeepSeek Vision API
            log.info("调用DeepSeek Vision API进行文字识别...");
            String ocrResult = llmApiService.visionWithDeepSeek(ocrPrompt, base64Image);

            log.info("DeepSeek Vision API识别成功，文本长度: {}", ocrResult.length());
            return ocrResult;

        } catch (Exception e) {
            log.error("DeepSeek Vision API调用失败", e);
            throw new AIServiceException("DeepSeek Vision API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 PDF 前若干页渲染为图片并逐页调用 Vision OCR（扫描件）
     */
    private String visionOcrPdfPages(MultipartFile file) throws Exception {
        String ocrPrompt = "请仔细识别图片中的所有文字内容，包括中文、英文、数字、标点符号等。" +
                "如果是法律文书，请特别注意：\n" +
                "1. 案号的准确性\n" +
                "2. 当事人姓名、公司名称\n" +
                "3. 日期格式\n" +
                "4. 金额数字\n" +
                "5. 法律条文引用\n\n" +
                "请按照原文的格式和排版输出识别结果，保持段落结构和换行。";

        try (org.apache.pdfbox.pdmodel.PDDocument document =
                     org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.rendering.PDFRenderer renderer =
                    new org.apache.pdfbox.rendering.PDFRenderer(document);
            int total = document.getNumberOfPages();
            if (total <= 0) {
                throw new AIServiceException("PDF 无页面可读");
            }
            int maxPages = Math.min(total, Math.max(1, pdfVisionMaxPages));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxPages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, pdfVisionDpi);
                img = limitImageWidth(img, 2048);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                log.info("Vision OCR PDF 第 {}/{} 页，编码长度 {}", i + 1, maxPages, b64.length());
                String part = llmApiService.visionWithDeepSeek(ocrPrompt, b64);
                sb.append("---第").append(i + 1).append("页---\n").append(part).append("\n\n");
            }
            if (total > maxPages) {
                sb.append("\n（仅识别前 ").append(maxPages).append(" 页，共 ").append(total).append(" 页）\n");
            }
            return sb.toString().trim();
        }
    }

    private static BufferedImage limitImageWidth(BufferedImage src, int maxWidth) {
        if (src.getWidth() <= maxWidth) {
            return src;
        }
        double scale = (double) maxWidth / src.getWidth();
        int w = maxWidth;
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    /**
     * 从PDF文件中提取文本（支持MultipartFile）
     */
    private String extractTextFromPDF(MultipartFile file) throws Exception {
        log.debug("开始使用PDFBox提取PDF文本");

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            StringBuilder text = new StringBuilder();

            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                text.append(pageText).append("\n");
            }

            String result = text.toString().trim();
            log.debug("PDFBox提取完成，页数: {}, 文本长度: {}", document.getNumberOfPages(), result.length());
            return result;
        }
    }

    /**
     * 使用LLM提取法律文书关键信息
     */
    private AIDocumentRecognitionResult extractLegalInfo(String ocrText, AIConfig aiConfig) throws Exception {
        String prompt = buildExtractionPrompt(ocrText);
        String llmResponse = llmApiService.chatWithConfig(prompt, null, aiConfig);
        return parseLLMResponse(llmResponse);
    }

    /**
     * 构建要素提取Prompt（深度优化版 - 增强文书类型识别准确性）
     */
    private String buildExtractionPrompt(String ocrText) {
        return String.format(
            "你是一个专业的法律文书信息提取助手。请从以下法院文书中提取关键信息，以JSON格式返回。\n\n" +
            "## 任务说明\n" +
            "1. **精准识别文书类型**：根据文书的**格式结构**和**关键词**进行判断\n" +
            "2. **提取关键信息**：根据文书类型提取对应的关键信息\n" +
            "3. **字段验证**：确保提取的信息符合法律文书的规范\n\n" +
            "## 文书类型识别规则\n" +
            "**判决书**：标题为\"民事判决书\"/\"刑事判决书\"/\"行政判决书\"，包含\"原告\"\"被告\"\"判决如下\"\"判决如下\"\n" +
            "**裁定书**：标题为\"民事裁定书\"/\"刑事裁定书\"/\"行政裁定书\"，包含\"裁定如下\"\"本院裁定\"\n" +
            "**起诉状**：包含\"民事起诉状\"\"诉讼请求\"\"事实与理由\"\"原告\"\"被告\"\n" +
            "**答辩状**：包含\"民事答辩状\"\"答辩意见\"\"答辩人\"\"被答辩人\"\n" +
            "**调解书**：包含\"民事调解书\"\"调解协议\"\"双方当事人\"\n" +
            "**传票**：包含\"传票\"\"开庭时间\"\"开庭地点\"\"案号\"\n" +
            "**通知书**：包含\"通知书\"\"通知事项\"（应诉通知书/举证通知书等）\n\n" +
            "## 需要提取的字段\n" +
            "- **caseNumber**: 案号（格式：(2024)京01民初123号）\n" +
            "- **courtName**: 法院名称（必填，如：北京市第一人民法院）\n" +
            "- **documentType**: 文书类型（必填）\n" +
            "  - 判决书、裁定书、起诉状、答辩状、调解书、传票、通知书、其他\n" +
            "- **plaintiffName**: 原告/申请人姓名或名称\n" +
            "- **defendantName**: 被告/被申请人姓名或名称\n" +
            "- **caseReason**: 案由（如：民间借贷纠纷、买卖合同纠纷）\n" +
            "- **judgmentDate**: 判决/裁定/调解日期（仅判决书/裁定书/调解书需要，格式：YYYY-MM-DD）\n" +
            "- **hearingDate**: 开庭时间（仅传票需要，格式：YYYY-MM-DD HH:mm）\n" +
            "- **hearingPlace**: 开庭地点/法庭号\n" +
            "- **judgeName**: 承办法官/审判员姓名\n" +
            "- **clerkName**: 书记员姓名\n" +
            "- **contactPhone**: 联系电话\n\n" +
            "## 字段验证规则\n" +
            "- 案号必须符合格式：(年份)法院代字民/刑/行初/终/他字第号码号\n" +
            "- 日期必须符合实际日期逻辑（月1-12，日1-31）\n" +
            "- 人名必须是中文2-10个字符\n" +
            "- 案由必须从\"民事案件案由规定\"中选择\n\n" +
            "## 文书内容\n" +
            "```\n%s\n```\n\n" +
            "## 输出要求\n" +
            "1. **严格返回JSON格式**，不要包含任何其他文字\n" +
            "2. 无法识别的字段填null\n" +
            "3. 日期格式必须严格按照示例格式：YYYY-MM-DD 或 YYYY-MM-DD HH:mm\n" +
            "4. 文书类型必须是上述列举的类型之一\n" +
            "5. 确保提取的信息准确，避免幻觉\n\n" +
            "现在请提取信息并返回JSON：",
            ocrText
        );
    }

    /**
     * 从已提取的纯文本做要素识别（Word/TXT 等）。
     */
    public AIDocumentRecognitionResult recognizeFromText(String text, Long userId, Long caseId) throws Exception {
        AIConfig aiConfig;
        try {
            aiConfig = aimodelRoutingService.resolveForUseCase(AIModelUseCase.DOCUMENT_RECOGNITION_EXTRACT);
        } catch (Exception e) {
            aiConfig = createDefaultOllamaConfig();
        }
        AIDocumentRecognitionResult result = extractLegalInfo(text, aiConfig);
        result.setOcrText(text);
        return result;
    }

    /**
     * 执行业务逻辑（根据文书类型路由）
     */
    public void executeBusinessLogic(AIDocumentRecognitionResult result, Long userId) {
        String docType = result.getDocumentType();

        if (docType == null || docType.isEmpty()) {
            log.warn("文书类型为空，跳过业务逻辑执行");
            return;
        }

        log.info("开始执行业务逻辑: 文书类型={}", docType);

        try {
            switch (docType) {
                case "判决书":
                    log.info("识别到判决书，执行判决书业务逻辑");
                    businessLogicHandler.handleJudgment(result, userId);
                    break;

                case "起诉状":
                    log.info("识别到起诉状，执行起诉状业务逻辑");
                    businessLogicHandler.handleComplaint(result, userId);
                    break;

                case "答辩状":
                    log.info("识别到答辩状，执行答辩状业务逻辑");
                    businessLogicHandler.handleAnswer(result, userId);
                    break;

                case "调解书":
                    log.info("识别到调解书，执行调解书业务逻辑");
                    businessLogicHandler.handleMediation(result, userId);
                    break;

                default:
                    log.info("文书类型 {} 暂不支持自动业务逻辑执行", docType);
                    break;
            }
        } catch (Exception e) {
            log.error("执行业务逻辑失败: 文书类型={}", docType, e);
            // 不抛出异常，避免影响识别结果返回
            // 前端可以根据 businessLogicExecuted 标志判断是否需要手动处理
        }
    }

    /**
     * 解析LLM响应
     */
    private AIDocumentRecognitionResult parseLLMResponse(String response) throws Exception {
        try {
            // 尝试直接解析JSON
            AIDocumentRecognitionResult result = objectMapper.readValue(response, AIDocumentRecognitionResult.class);

            // 验证和修正提取的字段
            validateAndCorrectFields(result);

            return result;
        } catch (Exception e) {
            // 如果解析失败，尝试提取JSON部分
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = response.substring(jsonStart, jsonEnd + 1);
                AIDocumentRecognitionResult result = objectMapper.readValue(jsonPart, AIDocumentRecognitionResult.class);

                // 验证和修正提取的字段
                validateAndCorrectFields(result);

                return result;
            }

            throw new AIServiceException("无法解析LLM响应: " + response);
        }
    }

    /**
     * 验证和修正提取的字段（深度优化版）
     */
    private void validateAndCorrectFields(AIDocumentRecognitionResult result) {
        // 1. 验证案号格式
        if (result.getCaseNumber() != null && !result.getCaseNumber().trim().isEmpty()) {
            String caseNumber = result.getCaseNumber().trim();
            // 移除案号中的多余空格和特殊字符
            caseNumber = caseNumber.replaceAll("\\s+", "");
            result.setCaseNumber(caseNumber);

            // 验证案号格式
            if (!caseNumber.matches(".*\\(\\d{4}\\).*第.*号")) {
                log.warn("案号格式可能不正确: {}", caseNumber);
            }
        }

        // 2. 验证日期格式
        if (result.getJudgmentDate() != null && !result.getJudgmentDate().trim().isEmpty()) {
            String judgmentDate = result.getJudgmentDate().trim();
            // 尝试修正常见的日期格式错误
            if (judgmentDate.matches("\\d{4}年\\d{1,2}月\\d{1,2}日")) {
                judgmentDate = judgmentDate.replaceAll("年", "-")
                                            .replaceAll("月", "-")
                                            .replaceAll("日", "");
                result.setJudgmentDate(judgmentDate);
            }
        }

        // 3. 验证人名格式
        if (result.getPlaintiffName() != null && result.getPlaintiffName().length() > 50) {
            log.warn("原告名称过长，可能提取错误: {}", result.getPlaintiffName());
            result.setPlaintiffName(result.getPlaintiffName().substring(0, 50));
        }

        if (result.getDefendantName() != null && result.getDefendantName().length() > 50) {
            log.warn("被告名称过长，可能提取错误: {}", result.getDefendantName());
            result.setDefendantName(result.getDefendantName().substring(0, 50));
        }

        // 4. 验证文书类型
        if (result.getDocumentType() != null) {
            String docType = result.getDocumentType().trim();
            // 标准化文书类型名称
            if (docType.contains("判决")) {
                result.setDocumentType("判决书");
            } else if (docType.contains("裁定")) {
                result.setDocumentType("裁定书");
            } else if (docType.contains("起诉") && docType.contains("状")) {
                result.setDocumentType("起诉状");
            } else if (docType.contains("答辩") && docType.contains("状")) {
                result.setDocumentType("答辩状");
            } else if (docType.contains("调解")) {
                result.setDocumentType("调解书");
            } else if (docType.contains("传票")) {
                result.setDocumentType("传票");
            } else if (docType.contains("通知")) {
                result.setDocumentType("通知书");
            } else {
                result.setDocumentType(docType);
            }
        }

        // 5. 记录验证日志
        log.info("字段验证完成: 案号={}, 文书类型={}",
            result.getCaseNumber(), result.getDocumentType());
    }

    /**
     * 创建默认的Ollama配置（当数据库中没有配置时使用）
     */
    private AIConfig createDefaultOllamaConfig() {
        AIConfig config = new AIConfig();
        config.setProviderType("ollama");
        config.setApiUrl("http://localhost:11434");
        config.setModelName("qwen3:8b");
        config.setMaxTokens(2000);
        config.setTemperature(0.1);
        return config;
    }
}