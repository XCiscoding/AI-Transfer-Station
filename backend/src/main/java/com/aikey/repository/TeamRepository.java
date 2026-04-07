package com.aikey.repository;

import com.aikey.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    Optional<Team> findByTeamCodeAndDeleted(String teamCode, Integer deleted);

    Optional<Team> findByIdAndDeleted(Long id, Integer deleted);

    List<Team> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);
}
