package com.lawfirm.service;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseDocument;
import com.lawfirm.repository.CaseDocumentRepository;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseDocumentService {

    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseRepository caseRepository;
    private final ObjectStorageService objectStorageService;

    private static final String UPLOAD_BASE_DIR = "uploads/documents/";

    /**
     * 上传案件文档
     */
    @Transactional
    public CaseDocumentDTO uploadDocument(Long caseId, MultipartFile file,
                                          String documentType, String folderPath,
                                          Long userId) throws IOException {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));

        String originalFilename = file.getOriginalFilename();
        String newFilename = buildStandardFileName(caseEntity, documentType, originalFilename);
        String filePath = storeFile(caseId, newFilename, file);

        // 创建文档记录
        CaseDocument document = new CaseDocument();
        document.setCaseId(caseId);
        document.setDocumentName(newFilename);
        document.setDocumentType(documentType);
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFolderPath(folderPath);
        document.setUploadBy(userId);
        document.setContentType(resolveContentType(file.getContentType(), originalFilename));
        int nextVersion = caseDocumentRepository.findMaxVersionNo(caseId, newFilename)
                .map(v -> v + 1).orElse(1);
        document.setVersionNo(nextVersion);

        CaseDocument saved = caseDocumentRepository.save(document);
        log.info("上传案件文档成功: caseId={}, fileName={}, path={}", caseId, newFilename, filePath);

        return convertToDTO(saved);
    }

    /**
     * 获取案件文档列表
     */
    public List<CaseDocumentDTO> getCaseDocuments(Long caseId) {
        return caseDocumentRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取文档列表
     */
    public List<CaseDocumentDTO> getDocumentsByType(String documentType) {
        return caseDocumentRepository.findByDocumentType(documentType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取全部文档（跨案件聚合视图）
     */
    public List<CaseDocumentDTO> getAllDocuments() {
        return caseDocumentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档详情
     */
    public CaseDocumentDTO getDocumentById(Long id) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        return convertToDTO(document);
    }

    /**
     * 更新文档信息
     */
    @Transactional
    public CaseDocumentDTO updateDocument(Long id, CaseDocumentDTO dto) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        document.setDocumentName(dto.getDocumentName());
        document.setDocumentType(dto.getDocumentType());
        document.setFolderPath(dto.getFolderPath());
        document.setTags(dto.getTags());
        document.setOcrResult(dto.getOcrResult());

        CaseDocument updated = caseDocumentRepository.save(document);
        log.info("更新案件文档成功: id={}", id);

        return convertToDTO(updated);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        // 删除物理文件
        try {
            if (isMinioPath(document.getFilePath())) {
                objectStorageService.delete(document.getFilePath());
            } else {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            }
        } catch (IOException e) {
            log.warn("删除物理文件失败: {}", document.getFilePath(), e);
        }

        // 删除数据库记录
        caseDocumentRepository.deleteById(id);
        log.info("删除案件文档成功: id={}", id);
    }

    /**
     * 移动文档到其他文件夹
     */
    @Transactional
    public CaseDocumentDTO moveDocument(Long id, String newFolderPath) {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        document.setFolderPath(newFolderPath);
        CaseDocument updated = caseDocumentRepository.save(document);

        log.info("移动案件文档成功: id={}, newFolder={}", id, newFolderPath);
        return convertToDTO(updated);
    }

    public String resolveContentType(CaseDocument document) {
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            return document.getContentType();
        }
        String name = document.getDocumentName();
        if (name == null) {
            return "application/octet-stream";
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain;charset=UTF-8";
        }
        return "application/octet-stream";
    }

    public InputStream openDocumentStream(Long id) throws IOException {
        CaseDocument document = caseDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (isMinioPath(document.getFilePath())) {
            return objectStorageService.download(document.getFilePath());
        }
        return Files.newInputStream(Paths.get(document.getFilePath()));
    }

    /**
     * 转换为DTO
     */
    private CaseDocumentDTO convertToDTO(CaseDocument document) {
        CaseDocumentDTO dto = new CaseDocumentDTO();
        dto.setId(document.getId());
        dto.setCaseId(document.getCaseId());
        dto.setDocumentName(document.getDocumentName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setFolderPath(document.getFolderPath());
        dto.setUploadBy(document.getUploadBy());
        dto.setTags(document.getTags());
        dto.setOcrResult(document.getOcrResult());
        dto.setVersionNo(document.getVersionNo());
        dto.setContentType(document.getContentType());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }

    private String storeFile(Long caseId, String newFilename, MultipartFile file) throws IOException {
        String objectName = "cases/" + caseId + "/" + newFilename;
        try {
            return objectStorageService.upload(objectName, file);
        } catch (RuntimeException e) {
            log.warn("MinIO不可用，回退到本地文档目录: {}", e.getMessage());
            Path uploadPath = Paths.get(UPLOAD_BASE_DIR, caseId.toString());
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        }
    }

    private String buildStandardFileName(Case caseEntity, String documentType, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String client = firstNonBlank(caseEntity.getEntrustingBankName(), caseEntity.getCaseName(), "案件");
        String title = firstNonBlank(documentType, "文书");
        return sanitize(client + "_" + caseEntity.getCaseName() + "_" + title + "_" + LocalDate.now() + extension);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private boolean isMinioPath(String filePath) {
        return filePath != null && filePath.startsWith("minio:");
    }

    private int nextVersionNo(Long caseId, String documentName) {
        return caseDocumentRepository.findMaxVersionNo(caseId, documentName)
                .map(max -> max + 1)
                .orElse(1);
    }

    private String resolveContentType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    public String resolvePreviewContentType(CaseDocument document) {
        if (document.getContentType() != null && !document.getContentType().isBlank()) {
            return document.getContentType();
        }
        return resolveContentType(null, document.getDocumentName());
    }
}
