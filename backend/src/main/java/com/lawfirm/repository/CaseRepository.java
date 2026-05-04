package com.lawfirm.repository;

import com.lawfirm.entity.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 案件Repository
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    /**
     * 根据案号查找案件
     */
    Optional<Case> findByCaseNumber(String caseNumber);

    /**
     * 根据主办律师ID查找案件列表
     */
    List<Case> findByOwnerId(Long ownerId);

    /**
     * 根据案件类型查找案件列表
     */
    List<Case> findByCaseType(String caseType);

    /**
     * 根据案件状态查找案件列表
     */
    List<Case> findByStatus(String status);

    /**
     * 根据案件等级查找案件列表
     */
    List<Case> findByLevel(String level);

    /**
     * 根据立案日期范围查找案件
     */
    List<Case> findByFilingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 检查案号是否存在
     */
    boolean existsByCaseNumber(String caseNumber);

    /**
     * 检查案号是否存在（未删除）
     */
    boolean existsByCaseNumberAndDeletedFalse(String caseNumber);

    /**
     * 全局搜索案件
     */
    @Query("SELECT c FROM Case c WHERE c.caseName LIKE %:keyword% OR c.caseNumber LIKE %:keyword% OR c.caseReason LIKE %:keyword%")
    Page<Case> searchCases(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计各状态案件数量
     */
    @Query("SELECT c.status, COUNT(c) FROM Case c GROUP BY c.status")
    List<Object[]> countByStatus();

    /**
     * 根据案件名称模糊查找
     */
    List<Case> findByCaseNameContainingAndDeletedFalse(String caseName);

    /**
     * 根据案号查找未删除的案件
     */
    Optional<Case> findByCaseNumberAndDeletedFalse(String caseNumber);

    /**
     * 根据案号查找未删除的案件列表（用于查重）
     */
    List<Case> findAllByCaseNumberAndDeletedFalse(String caseNumber);

    /**
     * 统计指定案件类型的未删除案件数量
     */
    long countByCaseTypeAndDeletedFalse(String caseType);

    /**
     * 根据主办律师查找未删除的案件
     */
    Page<Case> findByOwnerIdAndDeletedFalse(Long ownerId, Pageable pageable);

    /**
     * 根据案件状态查找未删除的案件
     */
    Page<Case> findByStatusAndDeletedFalse(String status, Pageable pageable);

    /**
     * 根据审限日期和状态查找案件
     */
    List<Case> findByDeadlineDateAndStatusAndDeletedFalse(LocalDate deadlineDate, String status);

    /**
     * 根据客户ID查找案件（用于关联检查）
     */
    List<Case> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /**
     * 根据创建日期范围查找未删除的案件（统计优化）
     */
    List<Case> findByCreatedAtBetweenAndDeletedFalseOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);

    /**
     * 查找律师费不为空的案件（财务统计优化）
     */
    @Query("SELECT c FROM Case c WHERE c.attorneyFee IS NOT NULL")
    List<Case> findByAttorneyFeeIsNotNull();

    /**
     * 查找所有未删除的案件（搜索优化）
     */
    @Query("SELECT c FROM Case c WHERE c.deleted = false")
    List<Case> findByDeletedFalse();

    /**
     * 查找不重复的法院列表（用于法院搜索）
     */
    @Query("SELECT DISTINCT c.court FROM Case c WHERE c.court IS NOT NULL AND c.court != '' AND c.deleted = false ORDER BY c.court")
    List<String> findDistinctCourts();

    /**
     * 根据关键词查找法院（模糊搜索）
     */
    @Query("SELECT DISTINCT c.court FROM Case c WHERE c.court IS NOT NULL AND c.court != '' AND c.deleted = false AND c.court LIKE %:keyword% ORDER BY c.court")
    List<String> findCourtsByKeyword(@Param("keyword") String keyword);

    // ==================== 利益冲突检查相关查询 ====================

    /**
     * 根据客户名称查找案号列表（用于利益冲突检查）
     * 查找该客户名称作为委托人的所有案件
     */
    @Query("SELECT DISTINCT c.caseNumber FROM Case c " +
            "JOIN Client cl ON cl.clientName = :clientName " +
            "WHERE c.clientId = cl.id AND c.deleted = false")
    List<String> findCaseNumbersByClientName(@Param("clientName") String clientName);

    /**
     * 根据对方当事人名称查找案号列表（用于利益冲突检查）
     * 查找该名称作为对方当事人的所有案件
     * 注意：由于当前系统中Case和Party可能没有直接的JPA关系，
     * 这里使用简化的实现，实际可能需要调整
     */
    @Query("SELECT DISTINCT c.caseNumber FROM Case c " +
            "WHERE c.caseName LIKE CONCAT('%', :partyName, '%') AND c.deleted = false")
    List<String> findCaseNumbersByOpposingPartyName(@Param("partyName") String partyName);
}
