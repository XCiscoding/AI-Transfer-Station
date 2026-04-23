package com.aikey.service;

import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.dto.team.TeamMemberAddRequest;
import com.aikey.dto.team.TeamMemberVO;
import com.aikey.dto.team.TeamOwnerTransferRequest;
import com.aikey.entity.Team;
import com.aikey.entity.TeamMember;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.TeamMemberRepository;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamMemberService {

    private static final String OWNER_ROLE = "owner";
    private static final String MEMBER_ROLE = "member";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<TeamMemberVO> listMembers(Long teamId) {
        Team team = getManageableTeam(teamId);
        return teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(team.getId()).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserCandidateVO> listCandidates(Long teamId, String keyword) {
        Team team = getManageableTeam(teamId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return userRepository.findByStatusAndDeleted(1, 0).stream()
                .filter(user -> !teamMemberRepository.existsByTeamIdAndUserId(team.getId(), user.getId()))
                .filter(user -> normalizedKeyword.isEmpty()
                        || user.getUsername().contains(normalizedKeyword)
                        || (user.getRealName() != null && user.getRealName().contains(normalizedKeyword)))
                .map(user -> new UserCandidateVO(user.getId(), user.getUsername(), user.getRealName()))
                .toList();
    }

    public List<TeamMemberVO> addMember(Long teamId, TeamMemberAddRequest request) {
        Team team = getManageableTeam(teamId);
        Long userId = request.getUserId();
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new BusinessException(400, "该用户已在团队中");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(MEMBER_ROLE)
                .joinedAt(LocalDateTime.now())
                .build();
        teamMemberRepository.save(member);
        syncMemberCount(team);
        return listMembers(team.getId());
    }

    public List<TeamMemberVO> removeMember(Long teamId, Long userId) {
        Team team = getManageableTeam(teamId);
        if (team.getOwner() != null && team.getOwner().getId().equals(userId)) {
            throw new BusinessException(400, "当前团队管理员不能直接移除，请先转交团队管理员");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(team.getId(), userId)
                .orElseThrow(() -> new BusinessException(404, "团队成员不存在"));
        teamMemberRepository.delete(member);
        syncMemberCount(team);
        return listMembers(team.getId());
    }

    public List<TeamMemberVO> transferOwner(Long teamId, TeamOwnerTransferRequest request) {
        Team team = getManageableTeam(teamId);
        User newOwner = userRepository.findById(request.getNewOwnerId())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (team.getOwner() != null && team.getOwner().getId().equals(newOwner.getId())) {
            throw new BusinessException(400, "该用户已经是当前团队管理员");
        }

        TeamMember newOwnerMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), newOwner.getId())
                .orElseGet(() -> teamMemberRepository.save(TeamMember.builder()
                        .team(team)
                        .user(newOwner)
                        .role(MEMBER_ROLE)
                        .joinedAt(LocalDateTime.now())
                        .build()));

        User oldOwner = team.getOwner();
        if (oldOwner != null) {
            teamMemberRepository.findByTeamIdAndUserId(team.getId(), oldOwner.getId())
                    .ifPresent(member -> member.setRole(MEMBER_ROLE));
        }

        newOwnerMember.setRole(OWNER_ROLE);
        team.setOwner(newOwner);
        team.setUpdatedAt(LocalDateTime.now());
        teamRepository.save(team);
        normalizeOwnerRole(team, newOwner.getId());
        syncMemberCount(team);
        return listMembers(team.getId());
    }

    public void ensureOwnerMembership(Team team) {
        if (team.getOwner() == null) {
            return;
        }
        teamMemberRepository.findByTeamIdAndUserId(team.getId(), team.getOwner().getId())
                .ifPresentOrElse(member -> member.setRole(OWNER_ROLE), () -> teamMemberRepository.save(TeamMember.builder()
                        .team(team)
                        .user(team.getOwner())
                        .role(OWNER_ROLE)
                        .joinedAt(LocalDateTime.now())
                        .build()));
        normalizeOwnerRole(team, team.getOwner().getId());
        syncMemberCount(team);
    }

    private Team getManageableTeam(Long teamId) {
        Team team = teamRepository.findByIdAndDeleted(teamId, 0)
                .orElseThrow(() -> new BusinessException(404, "团队不存在"));
        User currentUser = getCurrentUser();
        if (isSuperAdmin(currentUser)) {
            return team;
        }
        if (team.getOwner() == null || !team.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException(403, "仅团队管理员可操作该团队");
        }
        return team;
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
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }
        UserInfoResponse userInfo = authService.getUserInfo(user.getUsername());
        return Boolean.TRUE.equals(userInfo.getIsSuperAdmin());
    }

    private void normalizeOwnerRole(Team team, Long ownerUserId) {
        teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(team.getId()).forEach(member -> {
            if (member.getUser().getId().equals(ownerUserId)) {
                member.setRole(OWNER_ROLE);
            } else if (OWNER_ROLE.equals(member.getRole())) {
                member.setRole(MEMBER_ROLE);
            }
        });
    }

    private void syncMemberCount(Team team) {
        team.setMemberCount((int) teamMemberRepository.countByTeamId(team.getId()));
        team.setUpdatedAt(LocalDateTime.now());
        teamRepository.save(team);
    }

    private TeamMemberVO toVO(TeamMember member) {
        User user = member.getUser();
        return TeamMemberVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(member.getRole())
                .owner(OWNER_ROLE.equals(member.getRole()))
                .joinedAt(member.getJoinedAt())
                .build();
    }

    public record UserCandidateVO(Long id, String username, String realName) {}
}
