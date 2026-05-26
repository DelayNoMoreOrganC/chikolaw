package com.lawfirm.repository;

import com.lawfirm.entity.CaseDocument;
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

    List<CaseDocument> findByDeletedFalse();

    List<CaseDocument> findByDocumentType(String documentType);

    @Query("SELECT MAX(d.versionNo) FROM CaseDocument d WHERE d.caseId = :caseId AND d.documentName = :documentName AND d.deleted = false")
    Optional<Integer> findMaxVersionNo(@Param("caseId") Long caseId, @Param("documentName") String documentName);
}
