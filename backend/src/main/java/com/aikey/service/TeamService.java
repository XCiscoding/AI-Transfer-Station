package com.aikey.service;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.team.TeamCreateRequest;
import com.aikey.dto.team.TeamUpdateRequest;
import com.aikey.dto.team.TeamVO;
import com.aikey.entity.Team;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamVO createTeam(TeamCreateRequest request) {
        if (teamRepository.findByTeamCodeAndDeleted(request.getTeamCode(), 0).isPresent()) {
            throw new BusinessException("团队编码已存在");
        }

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDateTime now = LocalDateTime.now();
        Team team = Team.builder()
                .teamName(request.getTeamName())
                .teamCode(request.getTeamCode())
                .description(request.getDescription())
                .owner(owner)
                .quotaLimit(request.getQuotaLimit())
                .quotaRemaining(request.getQuotaLimit())
                .quotaWeight(request.getQuotaWeight())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toVO(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public PageResult<TeamVO> listTeams(int page, int size, String keyword) {
        Specification<Team> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.like(root.get("teamName"), "%" + keyword + "%"));
            }
            return predicates;
        };

        Page<Team> pageResult = teamRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        var voList = pageResult.getContent().stream().map(this::toVO).toList();

        return PageResult.<TeamVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    public TeamVO updateTeam(Long id, TeamUpdateRequest request) {
        Team team = teamRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("团队不存在"));

        if (StringUtils.hasText(request.getTeamName())) {
            team.setTeamName(request.getTeamName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (request.getQuotaLimit() != null) {
            BigDecimal newRemaining = request.getQuotaLimit().subtract(team.getQuotaUsed());
            team.setQuotaLimit(request.getQuotaLimit());
            team.setQuotaRemaining(newRemaining.max(BigDecimal.ZERO));
        }
        if (request.getQuotaWeight() != null) {
            team.setQuotaWeight(request.getQuotaWeight());
        }

        team.setUpdatedAt(LocalDateTime.now());
        return toVO(teamRepository.save(team));
    }

    public void deleteTeam(Long id) {
        Team team = teamRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("团队不存在"));
        team.setDeleted(1);
        team.setUpdatedAt(LocalDateTime.now());
        teamRepository.save(team);
    }

    private TeamVO toVO(Team team) {
        return TeamVO.builder()
                .id(team.getId())
                .teamName(team.getTeamName())
                .teamCode(team.getTeamCode())
                .description(team.getDescription())
                .ownerId(team.getOwner().getId())
                .ownerName(team.getOwner().getUsername())
                .memberCount(team.getMemberCount())
                .quotaLimit(team.getQuotaLimit())
                .quotaUsed(team.getQuotaUsed())
                .quotaRemaining(team.getQuotaRemaining())
                .quotaWeight(team.getQuotaWeight())
                .status(team.getStatus())
                .createdAt(team.getCreatedAt())
                .build();
    }
}
