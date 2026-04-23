package com.aikey.service;

import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.dto.common.PageResult;
import com.aikey.dto.virtualkey.VirtualKeyCreateRequest;
import com.aikey.dto.virtualkey.VirtualKeyUpdateRequest;
import com.aikey.dto.virtualkey.VirtualKeyVO;
import com.aikey.entity.Channel;
import com.aikey.entity.Project;
import com.aikey.entity.Team;
import com.aikey.entity.User;
import com.aikey.entity.VirtualKey;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.ProjectRepository;
import com.aikey.repository.TeamMemberRepository;
import com.aikey.repository.UserRepository;
import com.aikey.repository.VirtualKeyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.UUID;

/**
 * 虚拟Key管理服务类
 *
 * <p>提供虚拟API Key的生成、查询、更新、状态切换、刷新、删除等业务逻辑</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VirtualKeyService {

    private final VirtualKeyRepository virtualKeyRepository;

    private final UserRepository userRepository;

    private final ChannelRepository channelRepository;

    private final ProjectRepository projectRepository;

    private final TeamMemberRepository teamMemberRepository;

    private final TeamService teamService;

    private final ModelGroupService modelGroupService;

    private final ObjectMapper objectMapper;

    private final AuthService authService;

    /**
     * 生成虚拟Key
     *
     * <p>流程：验证用户存在 → 生成唯一Key值 → 构建实体 → 保存</p>
     *
     * @param request 创建请求
     * @return 虚拟Key VO
     */
    public VirtualKeyVO createVirtualKey(VirtualKeyCreateRequest request) {
        log.info("生成虚拟Key: name={}, userId={}", request.getKeyName(), request.getUserId());

        Team team = teamService.getManageableTeam(request.getTeamId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        validateTeamMember(team.getId(), user.getId());

        Project project = validateProject(team.getId(), request.getProjectId());

        validateAllowedGroupIds(request.getAllowedGroupIds());
        teamService.validateTeamGroupAccess(team.getId(), request.getAllowedGroupIds());
        validateChannel(request.getAllowedGroupIds(), request.getChannelId());

        String keyValue = generateUniqueKeyValue();

        LocalDateTime now = LocalDateTime.now();

        VirtualKey virtualKey = VirtualKey.builder()
                .keyName(request.getKeyName())
                .keyValue(keyValue)
                .user(user)
                .teamId(team.getId())
                .projectId(project.getId())
                .allowedModels(request.getAllowedModels())
                .allowedGroupIds(toJson(request.getAllowedGroupIds()))
                .channelId(request.getChannelId())
                .quotaType(request.getQuotaType())
                .quotaLimit(request.getQuotaLimit())
                .quotaUsed(BigDecimal.ZERO)
                .quotaRemaining(request.getQuotaLimit())
                .rateLimitQpm(request.getRateLimitQpm())
                .rateLimitQpd(request.getRateLimitQpd())
                .status(1)
                .expireTime(request.getExpireTime())
                .remark(request.getRemark())
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();

        VirtualKey savedKey = virtualKeyRepository.save(virtualKey);

        log.info("虚拟Key生成成功: id={}, keyValue={}", savedKey.getId(), savedKey.getKeyValue());
        return convertToVO(savedKey);
    }

    /**
     * 根据ID查询虚拟Key详情
     *
     * @param id Key ID
     * @return 虚拟Key VO
     */
    public VirtualKeyVO getVirtualKeyById(Long id) {
        log.debug("查询虚拟Key: id={}", id);

        VirtualKey virtualKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        // 检查是否已删除
        if (virtualKey.getDeleted() == 1) {
            throw new BusinessException("虚拟Key不存在");
        }
        validateReadAccess(virtualKey, getCurrentUser());

        return convertToVO(virtualKey);
    }

    /**
     * 分页查询虚拟Key列表
     *
     * <p>支持按用户ID、状态筛选，使用JPA Specification动态查询</p>
     *
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @param userId    用户ID筛选（可选）
     * @param status    状态筛选（可选，0-禁用，1-启用）
     * @param keyword   关键词搜索（可选，匹配Key名称）
     * @return 分页结果
     */
    public PageResult<VirtualKeyVO> listVirtualKeys(int page, int size, Long userId, Integer status, String keyword) {
        log.debug("查询虚拟Key列表: page={}, size={}, userId={}, status={}, keyword={}",
                page, size, userId, status, keyword);

        User currentUser = getCurrentUser();
        boolean superAdmin = isSuperAdmin(currentUser);
        boolean teamOwner = isTeamOwner(currentUser);
        List<Long> ownedTeamIds = teamOwner ? getOwnedTeamIds(currentUser) : Collections.emptyList();

        // 构建动态查询条件
        Specification<VirtualKey> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            // 始终添加未删除条件
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));

            // 用户ID精确匹配
            if (userId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("user").get("id"), userId));
            }

            if (!superAdmin && teamOwner) {
                if (ownedTeamIds.isEmpty()) {
                    predicates = cb.and(predicates, cb.disjunction());
                } else {
                    predicates = cb.and(predicates, root.get("teamId").in(ownedTeamIds));
                }
            } else if (!superAdmin) {
                predicates = cb.and(predicates, cb.equal(root.get("user").get("id"), currentUser.getId()));
            }

            // 状态精确匹配
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            // 关键词搜索：Key名称模糊匹配
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.like(root.get("keyName"), "%" + keyword + "%"));
            }

            return predicates;
        };

        // 分页查询（按创建时间降序）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VirtualKey> pageResult = virtualKeyRepository.findAll(spec, pageable);

        // 转换为VO列表
        var voList = pageResult.getContent().stream()
                .map(this::convertToVO)
                .toList();

        log.debug("查询虚拟Key列表完成: total={}", pageResult.getTotalElements());

        return PageResult.<VirtualKeyVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    /**
     * 更新虚拟Key配置
     *
     * @param id      Key ID
     * @param request 更新请求（只更新非空字段）
     * @return 更新后的虚拟Key VO
     */
    public VirtualKeyVO updateVirtualKey(Long id, VirtualKeyUpdateRequest request) {
        log.info("更新虚拟Key: id={}", id);

        // 查询虚拟Key
        VirtualKey virtualKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        // 检查是否已删除
        if (virtualKey.getDeleted() == 1) {
            throw new BusinessException("虚拟Key不存在");
        }
        validateManageAccess(virtualKey, getCurrentUser());

        try {
            if (request.getTeamId() != null && !request.getTeamId().equals(virtualKey.getTeamId())) {
                throw new BusinessException("不允许修改虚拟Key所属团队");
            }

            List<Long> finalAllowedGroupIds = request.getAllowedGroupIds() != null
                    ? request.getAllowedGroupIds()
                    : fromJson(virtualKey.getAllowedGroupIds());
            Long finalChannelId = request.getChannelId() != null ? request.getChannelId() : virtualKey.getChannelId();
            Long finalProjectId = request.getProjectId() != null ? request.getProjectId() : virtualKey.getProjectId();

            validateAllowedGroupIds(finalAllowedGroupIds);
            validateProject(virtualKey.getTeamId(), finalProjectId);
            teamService.validateTeamGroupAccess(virtualKey.getTeamId(), finalAllowedGroupIds);
            validateChannel(finalAllowedGroupIds, finalChannelId);

            // 逐字段更新非空值
            if (StringUtils.hasText(request.getKeyName())) {
                virtualKey.setKeyName(request.getKeyName());
            }
            virtualKey.setProjectId(finalProjectId);
            if (request.getAllowedModels() != null) {
                virtualKey.setAllowedModels(request.getAllowedModels());
            }
            virtualKey.setAllowedGroupIds(toJson(finalAllowedGroupIds));
            virtualKey.setChannelId(finalChannelId);
            if (StringUtils.hasText(request.getQuotaType())) {
                virtualKey.setQuotaType(request.getQuotaType());
            }
            if (request.getQuotaLimit() != null) {
                // 更新额度上限时，同步更新剩余额度
                BigDecimal quotaUsed = virtualKey.getQuotaUsed();
                BigDecimal newRemaining = request.getQuotaLimit().subtract(quotaUsed);
                virtualKey.setQuotaLimit(request.getQuotaLimit());
                virtualKey.setQuotaRemaining(newRemaining.max(BigDecimal.ZERO));
            }
            if (request.getRateLimitQpm() != null) {
                virtualKey.setRateLimitQpm(request.getRateLimitQpm());
            }
            if (request.getRateLimitQpd() != null) {
                virtualKey.setRateLimitQpd(request.getRateLimitQpd());
            }
            if (request.getExpireTime() != null) {
                virtualKey.setExpireTime(request.getExpireTime());
            }
            if (request.getRemark() != null) {
                virtualKey.setRemark(request.getRemark());
            }

            virtualKey.setUpdatedAt(LocalDateTime.now());
            VirtualKey updatedKey = virtualKeyRepository.save(virtualKey);

            log.info("虚拟Key更新成功: id={}", updatedKey.getId());
            return convertToVO(updatedKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新虚拟Key失败: id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException("更新虚拟Key失败: " + e.getMessage());
        }
    }

    /**
     * 切换虚拟Key状态（启用/禁用）
     *
     * @param id Key ID
     */
    public void toggleStatus(Long id) {
        log.info("切换虚拟Key状态: id={}", id);

        VirtualKey virtualKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        // 检查是否已删除
        if (virtualKey.getDeleted() == 1) {
            throw new BusinessException("虚拟Key不存在");
        }
        validateManageAccess(virtualKey, getCurrentUser());

        // 切换状态：1->0 或 0->1
        int newStatus = virtualKey.getStatus() == 1 ? 0 : 1;
        virtualKey.setStatus(newStatus);
        virtualKey.setUpdatedAt(LocalDateTime.now());
        virtualKeyRepository.save(virtualKey);

        log.info("虚拟Key状态切换成功: id={}, newStatus={}", id, newStatus);
    }

    /**
     * 刷新Key值（重新生成）
     *
     * <p>重新生成sk-xxx格式的Key值，保持其他配置不变</p>
     *
     * @param id Key ID
     * @return 更新后的虚拟Key VO
     */
    public VirtualKeyVO refreshKey(Long id) {
        log.info("刷新虚拟Key值: id={}", id);

        VirtualKey virtualKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        // 检查是否已删除
        if (virtualKey.getDeleted() == 1) {
            throw new BusinessException("虚拟Key不存在");
        }
        validateManageAccess(virtualKey, getCurrentUser());

        // 生成新的唯一Key值
        String newKeyValue = generateUniqueKeyValue();
        virtualKey.setKeyValue(newKeyValue);
        virtualKey.setUpdatedAt(LocalDateTime.now());

        VirtualKey refreshedKey = virtualKeyRepository.save(virtualKey);

        log.info("虚拟Key值刷新成功: id={}, newKeyValue={}", refreshedKey.getId(), refreshedKey.getKeyValue());
        return convertToVO(refreshedKey);
    }

    /**
     * 删除虚拟Key（逻辑删除）
     *
     * @param id Key ID
     */
    public void deleteVirtualKey(Long id) {
        log.info("删除虚拟Key: id={}", id);

        VirtualKey virtualKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        // 检查是否已删除
        if (virtualKey.getDeleted() == 1) {
            throw new BusinessException("虚拟Key不存在");
        }
        validateManageAccess(virtualKey, getCurrentUser());

        virtualKey.setDeleted(1);
        virtualKey.setUpdatedAt(LocalDateTime.now());
        virtualKeyRepository.save(virtualKey);

        log.info("虚拟Key删除成功: id={}", id);
    }

    /**
     * 生成唯一的Key值     *
     * <p>格式：sk- + UUID(去除横线).substring(0, 32)</p>
     * <p>循环生成直到找到不重复的keyValue</p>
     *
     * @return 唯一的Key值
     */
    private String generateUniqueKeyValue() {
        String keyValue;
        int retryCount = 0;
        final int MAX_RETRY = 10;

        do {
            keyValue = generateKeyValue();
            retryCount++;

            if (retryCount > MAX_RETRY) {
                log.error("生成唯一Key值失败，超过最大重试次数: {}", MAX_RETRY);
                throw new BusinessException("生成Key值失败，请稍后重试");
            }
        } while (virtualKeyRepository.existsByKeyValue(keyValue));

        return keyValue;
    }

    /**
     * 生成Key值
     *
     * @return sk-xxx格式的Key值
     */
    private String generateKeyValue() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "sk-" + uuid.substring(0, 32);
    }

    /**
     * 将VirtualKey实体转换为VO
     *
     * @param virtualKey 虚拟Key实体
     * @return 虚拟Key VO
     */
    private VirtualKeyVO convertToVO(VirtualKey virtualKey) {
        User user = virtualKey.getUser();
        Channel channel = loadChannel(virtualKey.getChannelId());
        return VirtualKeyVO.builder()
                .id(virtualKey.getId())
                .keyName(virtualKey.getKeyName())
                .keyValue(virtualKey.getKeyValue())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getUsername() : "未分配")
                .teamId(virtualKey.getTeamId())
                .projectId(virtualKey.getProjectId())
                .allowedModels(virtualKey.getAllowedModels())
                .allowedGroupIds(fromJson(virtualKey.getAllowedGroupIds()))
                .channelId(virtualKey.getChannelId())
                .channelName(channel != null ? channel.getChannelName() : null)
                .channelBaseUrl(channel != null ? channel.getBaseUrl() : null)
                .quotaType(virtualKey.getQuotaType())
                .quotaLimit(virtualKey.getQuotaLimit())
                .quotaUsed(virtualKey.getQuotaUsed())
                .quotaRemaining(virtualKey.getQuotaRemaining())
                .rateLimitQpm(virtualKey.getRateLimitQpm())
                .rateLimitQpd(virtualKey.getRateLimitQpd())
                .status(virtualKey.getStatus())
                .expireTime(virtualKey.getExpireTime())
                .lastUsedTime(virtualKey.getLastUsedTime())
                .remark(virtualKey.getRemark())
                .createdAt(virtualKey.getCreatedAt())
                .updatedAt(virtualKey.getUpdatedAt())
                .build();
    }

    private void validateAllowedGroupIds(List<Long> allowedGroupIds) {
        if (allowedGroupIds == null || allowedGroupIds.isEmpty()) {
            throw new BusinessException("请选择一个模型分组");
        }
        if (allowedGroupIds.size() > 1) {
            throw new BusinessException("虚拟Key仅允许绑定一个模型分组");
        }
    }

    private Project validateProject(Long teamId, Long projectId) {
        if (projectId == null) {
            throw new BusinessException("请选择所属项目");
        }
        Project project = projectRepository.findByIdAndDeleted(projectId, 0)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        if (project.getTeam() == null || !teamId.equals(project.getTeam().getId())) {
            throw new BusinessException("所选项目不属于当前团队");
        }
        return project;
    }

    private void validateTeamMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new BusinessException("所选用户不属于当前团队");
        }
    }

    private void validateReadAccess(VirtualKey virtualKey, User currentUser) {
        if (isSuperAdmin(currentUser)) {
            return;
        }
        if (isTeamOwner(currentUser) && getOwnedTeamIds(currentUser).contains(virtualKey.getTeamId())) {
            return;
        }
        User owner = virtualKey.getUser();
        if (owner != null && owner.getId().equals(currentUser.getId())) {
            return;
        }
        throw new BusinessException(403, "无权查看该虚拟Key");
    }

    private void validateManageAccess(VirtualKey virtualKey, User currentUser) {
        if (isSuperAdmin(currentUser)) {
            return;
        }
        if (isTeamOwner(currentUser) && getOwnedTeamIds(currentUser).contains(virtualKey.getTeamId())) {
            return;
        }
        throw new BusinessException(403, "普通用户只能领取和查看自己的虚拟Key，不能修改");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new BusinessException(401, "未获取到当前登录用户");
        }
        return userRepository.findWithRolesByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException(401, "当前登录用户不存在"));
    }

    private boolean isSuperAdmin(User user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }
        UserInfoResponse userInfo = authService.getUserInfo(user.getUsername());
        return Boolean.TRUE.equals(userInfo.getIsSuperAdmin());
    }

    private boolean isTeamOwner(User user) {
        return user != null && user.getId() != null && teamService.isTeamOwner(user.getId());
    }

    private List<Long> getOwnedTeamIds(User user) {
        if (user == null || user.getId() == null) {
            return Collections.emptyList();
        }
        return teamMemberRepository.findByUserIdOrderByJoinedAtAsc(user.getId()).stream()
                .filter(member -> "owner".equals(member.getRole()))
                .map(member -> member.getTeam().getId())
                .distinct()
                .toList();
    }

    private void validateChannel(List<Long> allowedGroupIds, Long channelId) {
        if (channelId == null) {
            throw new BusinessException("请选择路由渠道");
        }
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException("指定渠道不存在"));
        if (channel.getDeleted() == 1 || channel.getStatus() != 1) {
            throw new BusinessException("指定渠道不可用");
        }
        if (allowedGroupIds == null || allowedGroupIds.isEmpty()) {
            throw new BusinessException("请选择模型分组后再选择渠道");
        }
        Long groupId = allowedGroupIds.get(0);
        boolean matched = modelGroupService.listChannelsByGroupId(groupId).stream()
                .anyMatch(item -> item.getId().equals(channelId));
        if (!matched) {
            throw new BusinessException("所选渠道不属于当前模型分组");
        }
    }

    private Channel loadChannel(Long channelId) {
        if (channelId == null) {
            return null;
        }
        return channelRepository.findById(channelId)
                .filter(channel -> channel.getDeleted() == 0)
                .orElse(null);
    }

    private String toJson(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> fromJson(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
