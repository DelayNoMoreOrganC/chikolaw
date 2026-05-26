package com.lawfirm.repository;

import com.lawfirm.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 根据接收人查找通知列表
     */
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    /**
     * 查找未读通知
     */
    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);

    /**
     * 统计未读通知数量
     */
    Long countByReceiverIdAndIsReadFalse(Long receiverId);

    /**
     * 根据分类查找通知
     */
    Page<Notification> findByReceiverIdAndCategoryOrderByCreatedAtDesc(Long receiverId, String category, Pageable pageable);

    /**
     * 按聚合分类筛选
     */
    Page<Notification> findByReceiverIdAndCategoryGroupOrderByCreatedAtDesc(
            Long receiverId, String categoryGroup, Pageable pageable);

    long countByReceiverIdAndCategoryGroupAndIsReadFalse(Long receiverId, String categoryGroup);
}
