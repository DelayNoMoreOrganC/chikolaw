package com.lawfirm.repository;

import com.lawfirm.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 日程Repository
 */
@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    /**
     * 根据创建人查找日程
     */
    List<Calendar> findByCreatedByOrderByStartTimeAsc(Long createdBy);

    /**
     * 根据案件ID查找日程
     */
    List<Calendar> findByCaseIdOrderByStartTimeAsc(Long caseId);

    Optional<Calendar> findFirstByCaseIdAndSyncSourceAndDeletedFalse(Long caseId, String syncSource);

    /**
     * 根据时间范围查找日程
     */
    @Query("SELECT c FROM Calendar c WHERE c.startTime >= :start AND c.endTime <= :end")
    List<Calendar> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找指定时间后的日程
     */
    @Query("SELECT c FROM Calendar c WHERE c.startTime >= :start ORDER BY c.startTime ASC")
    List<Calendar> findUpcomingCalendars(@Param("start") LocalDateTime start);

    /**
     * 查找用户相关的日程（创建者或参与者）
     */
    @Query("SELECT c FROM Calendar c WHERE c.createdBy = :userId OR c.participants LIKE %:userId% ORDER BY c.startTime ASC")
    List<Calendar> findByUser(@Param("userId") Long userId);

    /**
     * 根据日期范围和类型查找未删除的日程（性能优化）
     */
    @Query("SELECT c FROM Calendar c WHERE c.deleted = false AND c.calendarType = :calendarType AND c.startTime >= :start AND c.startTime <= :end")
    List<Calendar> findByDeletedFalseAndCalendarTypeAndStartTimeBetween(@Param("calendarType") String calendarType, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计用户相关的日程数量（分页优化）
     */
    @Query("SELECT COUNT(c) FROM Calendar c WHERE c.createdBy = :userId OR c.participants LIKE %:userId%")
    long countByUser(@Param("userId") Long userId);
}
