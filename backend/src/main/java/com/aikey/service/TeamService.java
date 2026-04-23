package com.aikey.service;

import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.dto.common.PageResult;
import com.aikey.dto.modelgroup.ModelGroupVO;
import com.aikey.dto.team.TeamCreateRequest;
import com.aikey.dto.team.TeamUpdateRequest;
import com.aikey.dto.team.TeamVO;
import com.aikey.entity.Team;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.TeamMemberRepository;
import com.aikey.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ModelGroupService modelGroupService;
    private final TeamMemberService teamMemberService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public boolean isTeamOwner(Long userId) {
        return userId != null && teamRepository.existsByOwnerIdAndDeleted(userId, 0);
    }

    public TeamVO createTeam(TeamCreateRequest request) {
        User currentUser = getCurrentUser();
        UserInfoResponse userInfo = authService.getUserInfo(currentUser.getUsername());
        log.info("createTeam auth check -> username={}, userId={}, roles={}, isSuperAdmin={}, teamName={}, teamCode={}",
                userInfo.getUsername(),
                userInfo.getUserId(),
                userInfo.getRoles(),
                userInfo.getIsSuperAdmin(),
                request.getTeamName(),
                request.getTeamCode());
        if (!Boolean.TRUE.equals(userInfo.getIsSuperAdmin())) {
            throw new BusinessException(403, "仅企业管理员可创建团队");
        }
        if (!StringUtils.hasText(request.getTeamName())) {
            throw new BusinessException("团队名称不能为空");
        }
        if (!StringUtils.hasText(request.getTeamCode())) {
            throw new BusinessException("团队编码不能为空");
        }
        if (teamRepository.findByTeamCodeAndDeleted(request.getTeamCode(), 0).isPresent()) {
            throw new BusinessException("团队编码已存在");
        }

        User owner = resolveTeamOwner(currentUser, request.getOwnerId());

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

        Team savedTeam = teamRepository.save(team);
        teamMemberService.ensureOwnerMembership(savedTeam);
        return toVO(loadTeamWithOwner(savedTeam.getId()));
    }

    @Transactional(readOnly = true)
    public PageResult<TeamVO> listTeams(int page, int size, String keyword) {
        User currentUser = getCurrentUser();
        boolean superAdmin = isSuperAdmin(currentUser);
        boolean teamOwner = isTeamOwner(currentUser.getId());
        List<Long> readableTeamIds = superAdmin || teamOwner
                ? Collections.emptyList()
                : getMemberTeamIds(currentUser.getId());

        Specification<Team> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));
            if (!superAdmin && teamOwner) {
                predicates = cb.and(predicates, cb.equal(root.get("owner").get("id"), currentUser.getId()));
            } else if (!superAdmin) {
                if (readableTeamIds.isEmpty()) {
                    predicates = cb.and(predicates, cb.disjunction());
                } else {
                    predicates = cb.and(predicates, root.get("id").in(readableTeamIds));
                }
            }
            if (StringUtils.hasText(keyword)) {
                if (superAdmin) {
                    predicates = cb.and(predicates, cb.or(
                            cb.like(root.get("teamName"), "%" + keyword + "%"),
                            cb.like(root.get("teamCode"), "%" + keyword + "%")
                    ));
                }
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

    @Transactional(readOnly = true)
    public List<ModelGroupVO> getAllowedModelGroups(Long id) {
        Team team = getManageableTeam(id);
        return buildAllowedGroups(team);
    }

    public TeamVO updateTeam(Long id, TeamUpdateRequest request) {
        Team team = getManageableTeam(id);
        User currentUser = getCurrentUser();
        boolean superAdmin = isSuperAdmin(currentUser);

        if (StringUtils.hasText(request.getTeamName())) {
            team.setTeamName(request.getTeamName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (superAdmin) {
            if (request.getOwnerId() != null) {
                User owner = resolveTeamOwner(currentUser, request.getOwnerId());
                team.setOwner(owner);
                teamMemberService.ensureOwnerMembership(team);
            }
            if (request.getQuotaLimit() != null) {
                BigDecimal newRemaining = request.getQuotaLimit().subtract(team.getQuotaUsed());
                team.setQuotaLimit(request.getQuotaLimit());
                team.setQuotaRemaining(newRemaining.max(BigDecimal.ZERO));
            }
            if (request.getQuotaWeight() != null) {
                team.setQuotaWeight(request.getQuotaWeight());
            }
            if (request.getAllowedGroupIds() != null) {
                validateAllowedGroupIds(request.getAllowedGroupIds());
                team.setAllowedGroupIds(toJson(request.getAllowedGroupIds()));
            }
        }

        team.setUpdatedAt(LocalDateTime.now());
        Team savedTeam = teamRepository.save(team);
        return toVO(loadTeamWithOwner(savedTeam.getId()));
    }

    public void deleteTeam(Long id) {
        Team team = getManageableTeam(id);
        User currentUser = getCurrentUser();
        if (!isSuperAdmin(currentUser)) {
            throw new BusinessException(403, "仅企业管理员可删除团队");
        }
        team.setDeleted(1);
        team.setUpdatedAt(LocalDateTime.now());
        teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Team getManageableTeam(Long teamId) {
        Team team = teamRepository.findByIdAndDeleted(teamId, 0)
                .orElseThrow(() -> new BusinessException("团队不存在"));

        User currentUser = getCurrentUser();
        if (isSuperAdmin(currentUser)) {
            return team;
        }
        if (team.getOwner() == null || !team.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException(403, "仅团队管理员可操作该团队");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public void validateTeamGroupAccess(Long teamId, List<Long> allowedGroupIds) {
        if (allowedGroupIds == null || allowedGroupIds.isEmpty()) {
            return;
        }
        Team team = getManageableTeam(teamId);
        List<Long> teamAllowedGroupIds = parseIds(team.getAllowedGroupIds());
        if (teamAllowedGroupIds.isEmpty()) {
            throw new BusinessException("当前团队尚未配置可用模型分组");
        }
        boolean invalid = allowedGroupIds.stream().anyMatch(id -> !teamAllowedGroupIds.contains(id));
        if (invalid) {
            throw new BusinessException("所选模型分组不属于当前团队");
        }
    }

    private void validateAllowedGroupIds(List<Long> allowedGroupIds) {
        if (allowedGroupIds.isEmpty()) {
            return;
        }
        List<Long> enabledGroupIds = modelGroupService.listAll().stream()
                .map(ModelGroupVO::getId)
                .toList();
        boolean invalid = allowedGroupIds.stream().anyMatch(id -> !enabledGroupIds.contains(id));
        if (invalid) {
            throw new BusinessException("存在不可用的模型分组");
        }
    }

    private User resolveTeamOwner(User currentUser, Long ownerId) {
        if (ownerId == null) {
            return currentUser;
        }
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private Team loadTeamWithOwner(Long teamId) {
        return teamRepository.findByIdAndDeleted(teamId, 0)
                .orElseThrow(() -> new BusinessException("团队不存在"));
    }

    private TeamVO toVO(Team team) {
        User owner = team.getOwner();
        List<Long> allowedGroupIds = parseIds(team.getAllowedGroupIds());
        List<ModelGroupVO> allowedGroups = buildAllowedGroups(team);
        return TeamVO.builder()
                .id(team.getId())
                .teamName(team.getTeamName())
                .teamCode(team.getTeamCode())
                .description(team.getDescription())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerName(owner != null ? owner.getUsername() : "未分配")
                .memberCount(team.getMemberCount())
                .quotaLimit(team.getQuotaLimit())
                .quotaUsed(team.getQuotaUsed())
                .quotaRemaining(team.getQuotaRemaining())
                .quotaWeight(team.getQuotaWeight())
                .status(team.getStatus())
                .allowedGroupIds(allowedGroupIds)
                .allowedGroups(allowedGroups)
                .createdAt(team.getCreatedAt())
                .build();
    }

    private List<ModelGroupVO> buildAllowedGroups(Team team) {
        List<Long> allowedGroupIds = parseIds(team.getAllowedGroupIds());
        if (allowedGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return modelGroupService.listAll().stream()
                .filter(group -> allowedGroupIds.contains(group.getId()))
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new BusinessException(401, "未获取到当前登录用户");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException(401, "当前登录用户不存在"));
    }

    private boolean isSuperAdmin(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        UserInfoResponse userInfo = authService.getUserInfo(user.getUsername());
        return Boolean.TRUE.equals(userInfo.getIsSuperAdmin());
    }

    private List<Long> getMemberTeamIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return teamMemberRepository.findByUserIdOrderByJoinedAtAsc(userId).stream()
                .map(member -> member.getTeam().getId())
                .distinct()
                .toList();
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BusinessException("团队模型分组保存失败");
        }
    }

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析团队模型分组失败: {}", json);
            return Collections.emptyList();
        }
    }
}
