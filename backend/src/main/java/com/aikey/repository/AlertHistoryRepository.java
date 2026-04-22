package com.aikey.repository;

import com.aikey.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    Page<AlertHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByRuleIdAndCreatedAtAfter(Long ruleId, LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);

    List<AlertHistory> findTop10ByOrderByCreatedAtDesc();
}
