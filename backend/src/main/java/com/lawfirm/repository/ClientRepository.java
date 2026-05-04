package com.lawfirm.repository;

import com.lawfirm.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 客户Repository
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * 根据客户类型查找客户列表
     */
    List<Client> findByClientType(String clientType);

    /**
     * 根据客户状态查找客户列表
     */
    List<Client> findByStatus(String status);

    /**
     * 根据手机号查找客户
     */
    Optional<Client> findByPhone(String phone);

    /**
     * 根据身份证号查找客户
     */
    Optional<Client> findByIdCard(String idCard);

    /**
     * 根据信用代码查找客户
     */
    Optional<Client> findByCreditCode(String creditCode);

    /**
     * 全局搜索客户
     */
    @Query("SELECT c FROM Client c WHERE c.clientName LIKE %:keyword% OR c.phone LIKE %:keyword% OR c.idCard LIKE %:keyword%")
    Page<Client> searchClients(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 检查客户姓名是否存在
     */
    boolean existsByClientName(String clientName);

    /**
     * 检查客户姓名是否存在（仅未删除的）
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c WHERE c.clientName = :clientName AND c.deleted = false")
    boolean existsByClientNameAndDeletedIsFalse(@Param("clientName") String clientName);

    /**
     * 查找所有未删除的客户（搜索优化）
     */
    @Query("SELECT c FROM Client c WHERE c.deleted = false")
    List<Client> findByDeletedFalse();

    /**
     * 根据客户类型查找未删除的客户（性能优化）
     */
    @Query("SELECT c FROM Client c WHERE c.clientType = :clientType AND c.deleted = false")
    List<Client> findByClientTypeAndDeletedFalse(@Param("clientType") String clientType);

    /**
     * 根据客户状态查找未删除的客户（性能优化）
     */
    @Query("SELECT c FROM Client c WHERE c.status = :status AND c.deleted = false")
    List<Client> findByStatusAndDeletedFalse(@Param("status") String status);

    /**
     * 根据负责人查找未删除的客户（性能优化）
     */
    @Query("SELECT c FROM Client c WHERE c.ownerId = :ownerId AND c.deleted = false")
    List<Client> findByOwnerIdAndDeletedFalse(@Param("ownerId") Long ownerId);

    /**
     * 根据名称模糊查询客户名称列表（用于利益冲突检查）
     */
    @Query("SELECT c.clientName FROM Client c WHERE c.clientName LIKE CONCAT('%', :name, '%') AND c.deleted = false")
    List<String> findByNameContaining(@Param("name") String name);

    /**
     * 获取所有客户名称列表（用于利益冲突检查）
     */
    @Query("SELECT c.clientName FROM Client c WHERE c.deleted = false")
    List<String> findAllNames();
}
