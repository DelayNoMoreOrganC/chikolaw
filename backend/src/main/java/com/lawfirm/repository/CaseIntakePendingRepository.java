package com.lawfirm.repository;

import com.lawfirm.entity.CaseIntakePending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseIntakePendingRepository extends JpaRepository<CaseIntakePending, Long> {

    List<CaseIntakePending> findByUserIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            Long userId, String status);

    Optional<CaseIntakePending> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
}
