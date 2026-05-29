package com.lawfirm.repository;

import com.lawfirm.entity.CaseDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 案件文档Repository
 */
@Repository
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    List<CaseDocument> findByCaseIdOrderByCreatedAtDesc(Long caseId);

    List<CaseDocument> findByCaseIdAndDeletedFalseOrderByCreatedAtDesc(Long caseId);

    List<CaseDocument> findByDeletedFalse();

    List<CaseDocument> findByDocumentType(String documentType);

    List<CaseDocument> findByDocumentTypeAndDeletedFalse(String documentType);

    @Query("SELECT d FROM CaseDocument d WHERE d.deleted = false "
            + "AND (:caseId IS NULL OR d.caseId = :caseId) "
            + "AND (:documentType IS NULL OR :documentType = '' OR d.documentType = :documentType) "
            + "AND (:keyword IS NULL OR :keyword = '' OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CaseDocument> searchActiveDocuments(
            @Param("caseId") Long caseId,
            @Param("documentType") String documentType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COUNT(d) FROM CaseDocument d WHERE d.deleted = false")
    long countActiveDocuments();

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM CaseDocument d WHERE d.deleted = false")
    long sumActiveFileSize();

    @Query("SELECT COUNT(DISTINCT d.caseId) FROM CaseDocument d WHERE d.deleted = false")
    long countDistinctActiveCases();

    @Query("SELECT MAX(d.versionNo) FROM CaseDocument d WHERE d.caseId = :caseId AND d.documentName = :documentName AND d.deleted = false")
    Optional<Integer> findMaxVersionNo(@Param("caseId") Long caseId, @Param("documentName") String documentName);
}
