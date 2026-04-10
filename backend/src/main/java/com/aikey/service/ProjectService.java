package com.aikey.service;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.project.ProjectCreateRequest;
import com.aikey.dto.project.ProjectUpdateRequest;
import com.aikey.dto.project.ProjectVO;
import com.aikey.entity.Project;
import com.aikey.entity.Role;
import com.aikey.entity.Team;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ProjectRepository;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.UserRepository;
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
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public ProjectVO createProject(ProjectCreateRequest request) {
        if (projectRepository.findByProjectCodeAndDeleted(request.getProjectCode(), 0).isPresent()) {
            throw new BusinessException("项目编码已存在");
        }

        User currentUser = getCurrentUser();
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Team team = resolveProjectTeam(currentUser, request.getTeamId());

        LocalDateTime now = LocalDateTime.now();
        Project project = Project.builder()
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .description(request.getDescription())
                .team(team)
                .owner(owner)
                .quotaLimit(request.getQuotaLimit())
                .quotaRemaining(request.getQuotaLimit())
                .quotaWeight(request.getQuotaWeight())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toVO(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public PageResult<ProjectVO> listProjects(int page, int size, String keyword, Long teamId) {
        User currentUser = getCurrentUser();
        boolean superAdmin = isSuperAdmin(currentUser);
        Long scopedTeamId = teamId;
        if (!superAdmin) {
            scopedTeamId = getOwnedTeam(currentUser).getId();
        }

        Long finalScopedTeamId = scopedTeamId;
        Specification<Project> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.like(root.get("projectName"), "%" + keyword + "%"));
            }
            if (finalScopedTeamId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("team").get("id"), finalScopedTeamId));
            }
            return predicates;
        };

        Page<Project> pageResult = projectRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        var voList = pageResult.getContent().stream().map(this::toVO).toList();

        return PageResult.<ProjectVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    public ProjectVO updateProject(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("项目不存在"));

        User currentUser = getCurrentUser();
        validateProjectAccess(project, currentUser);

        if (StringUtils.hasText(request.getProjectName())) {
            project.setProjectName(request.getProjectName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (isSuperAdmin(currentUser)) {
            if (request.getTeamId() != null) {
                Team team = teamRepository.findByIdAndDeleted(request.getTeamId(), 0)
                        .orElseThrow(() -> new BusinessException("团队不存在"));
                project.setTeam(team);
            }
        } else {
            project.setTeam(getOwnedTeam(currentUser));
        }
        if (request.getQuotaLimit() != null) {
            BigDecimal newRemaining = request.getQuotaLimit().subtract(project.getQuotaUsed());
            project.setQuotaLimit(request.getQuotaLimit());
            project.setQuotaRemaining(newRemaining.max(BigDecimal.ZERO));
        }
        if (request.getQuotaWeight() != null) {
            project.setQuotaWeight(request.getQuotaWeight());
        }

        project.setUpdatedAt(LocalDateTime.now());
        return toVO(projectRepository.save(project));
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        validateProjectAccess(project, getCurrentUser());
        project.setDeleted(1);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    private Team resolveProjectTeam(User currentUser, Long requestedTeamId) {
        if (isSuperAdmin(currentUser)) {
            if (requestedTeamId == null) {
                return null;
            }
            return teamRepository.findByIdAndDeleted(requestedTeamId, 0)
                    .orElseThrow(() -> new BusinessException("团队不存在"));
        }
        return getOwnedTeam(currentUser);
    }

    private void validateProjectAccess(Project project, User currentUser) {
        if (isSuperAdmin(currentUser)) {
            return;
        }
        Team ownedTeam = getOwnedTeam(currentUser);
        if (project.getTeam() == null || !ownedTeam.getId().equals(project.getTeam().getId())) {
            throw new BusinessException("仅团队管理员可操作本团队项目");
        }
    }

    private Team getOwnedTeam(User user) {
        var teams = teamRepository.findByOwnerIdAndDeleted(user.getId(), 0);
        if (teams == null || teams.isEmpty()) {
            throw new BusinessException("当前用户未绑定团队");
        }
        return teams.get(0);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new BusinessException("未获取到当前登录用户");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("当前登录用户不存在"));
    }

    private boolean isSuperAdmin(User user) {
        Set<Role> roles = user.getRoles();
        return roles != null && roles.stream().map(Role::getRoleCode).anyMatch(SUPER_ADMIN_ROLE::equals);
    }

    private ProjectVO toVO(Project project) {
        return ProjectVO.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .projectCode(project.getProjectCode())
                .description(project.getDescription())
                .teamId(project.getTeam() != null ? project.getTeam().getId() : null)
                .teamName(project.getTeam() != null ? project.getTeam().getTeamName() : null)
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getUsername())
                .quotaLimit(project.getQuotaLimit())
                .quotaUsed(project.getQuotaUsed())
                .quotaRemaining(project.getQuotaRemaining())
                .quotaWeight(project.getQuotaWeight())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
