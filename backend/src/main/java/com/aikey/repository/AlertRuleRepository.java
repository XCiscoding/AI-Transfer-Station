package com.aikey.repository;

import com.aikey.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    Page<AlertRule> findByDeletedOrderByCreatedAtDesc(Integer deleted, Pageable pageable);

    List<AlertRule> findByIsEnabledAndDeleted(Integer isEnabled, Integer deleted);
}
