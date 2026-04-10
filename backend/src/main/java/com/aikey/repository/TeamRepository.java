package com.aikey.repository;

import com.aikey.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    Optional<Team> findByTeamCodeAndDeleted(String teamCode, Integer deleted);

    @EntityGraph(attributePaths = "owner")
    Optional<Team> findByIdAndDeleted(Long id, Integer deleted);

    @EntityGraph(attributePaths = "owner")
    List<Team> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    List<Team> findByOwnerUsernameAndDeleted(String username, Integer deleted);

    boolean existsByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    @EntityGraph(attributePaths = "owner")
    Page<Team> findAll(Specification<Team> spec, Pageable pageable);
}
