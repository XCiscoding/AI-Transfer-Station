package com.aikey.service;

import com.aikey.dto.channel.ChannelVO;
import com.aikey.dto.common.PageResult;
import com.aikey.dto.modelgroup.ModelGroupCreateRequest;
import com.aikey.dto.modelgroup.ModelGroupUpdateRequest;
import com.aikey.dto.modelgroup.ModelGroupVO;
import com.aikey.entity.Channel;
import com.aikey.entity.ModelGroup;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ModelGroupRepository;
import com.aikey.repository.ModelRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型分组业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelGroupService {

    private final ModelGroupRepository modelGroupRepository;
    private final ModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    // ===== 创建 =====

    @Transactional
    public ModelGroupVO create(ModelGroupCreateRequest req) {
        if (modelGroupRepository.existsByGroupNameAndDeleted(req.getGroupName(), 0)) {
            throw new BusinessException("分组名称已存在：" + req.getGroupName());
        }
        String modelIdsJson = toJson(req.getModelIds());
        ModelGroup group = ModelGroup.builder()
                .groupName(req.getGroupName())
                .description(req.getDescription())
                .modelIds(modelIdsJson)
                .status(1)
                .deleted(0)
                .build();
        return convertToVO(modelGroupRepository.save(group));
    }

    // ===== 查询单条 =====

    public ModelGroupVO getById(Long id) {
        ModelGroup group = modelGroupRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("模型分组不存在：" + id));
        return convertToVO(group);
    }

    // ===== 分页查询 =====

    public PageResult<ModelGroupVO> list(int page, int size, String keyword) {
        Specification<ModelGroup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("groupName"), "%" + keyword + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ModelGroup> pageResult = modelGroupRepository.findAll(
                spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<ModelGroupVO> records = pageResult.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.<ModelGroupVO>builder()
                .records(records)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    // ===== 更新 =====

    @Transactional
    public ModelGroupVO update(Long id, ModelGroupUpdateRequest req) {
        ModelGroup group = modelGroupRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("模型分组不存在：" + id));

        if (StringUtils.hasText(req.getGroupName()) && !req.getGroupName().equals(group.getGroupName())) {
            if (modelGroupRepository.existsByGroupNameAndDeleted(req.getGroupName(), 0)) {
                throw new BusinessException("分组名称已存在：" + req.getGroupName());
            }
            group.setGroupName(req.getGroupName());
        }
        if (req.getDescription() != null) {
            group.setDescription(req.getDescription());
        }
        if (req.getModelIds() != null && !req.getModelIds().isEmpty()) {
            group.setModelIds(toJson(req.getModelIds()));
        }
        if (req.getStatus() != null) {
            group.setStatus(req.getStatus());
        }

        return convertToVO(modelGroupRepository.save(group));
    }

    // ===== 逻辑删除 =====

    @Transactional
    public void delete(Long id) {
        ModelGroup group = modelGroupRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("模型分组不存在：" + id));
        group.setDeleted(1);
        modelGroupRepository.save(group);
    }

    // ===== 全部启用分组（下拉选择） =====

    public List<ModelGroupVO> listAll() {
        Specification<ModelGroup> spec = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("deleted"), 0),
                        cb.equal(root.get("status"), 1)
                );
        return modelGroupRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "groupName"))
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    public List<ChannelVO> listChannelsByGroupId(Long groupId) {
        ModelGroup group = modelGroupRepository.findByIdAndDeleted(groupId, 0)
                .orElseThrow(() -> new BusinessException("模型分组不存在：" + groupId));

        List<Long> modelIds = parseIds(group.getModelIds());
        if (modelIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, ChannelVO> channelMap = new LinkedHashMap<>();
        modelRepository.findAllById(modelIds).forEach(model -> {
            if (model == null || model.getDeleted() == 1 || model.getStatus() != 1) {
                return;
            }
            Channel channel = model.getChannel();
            if (channel == null || channel.getDeleted() == 1 || channel.getStatus() != 1) {
                return;
            }
            if (channel.getHealthStatus() != null && channel.getHealthStatus() != 1) {
                return;
            }
            channelMap.putIfAbsent(channel.getId(), ChannelVO.builder()
                    .id(channel.getId())
                    .channelName(channel.getChannelName())
                    .channelCode(channel.getChannelCode())
                    .channelType(channel.getChannelType())
                    .baseUrl(channel.getBaseUrl())
                    .weight(channel.getWeight())
                    .priority(channel.getPriority())
                    .status(channel.getStatus())
                    .healthStatus(channel.getHealthStatus())
                    .createdAt(channel.getCreatedAt())
                    .build());
        });

        return new ArrayList<>(channelMap.values());
    }

    // ===== 工具：转 VO =====

    private ModelGroupVO convertToVO(ModelGroup group) {
        List<Long> modelIds = parseIds(group.getModelIds());

        // 查询模型详情，不存在的跳过
        List<ModelGroupVO.ModelSimpleVO> models = modelIds.stream()
                .map(mid -> modelRepository.findById(mid).orElse(null))
                .filter(m -> m != null && m.getDeleted() == 0)
                .map(m -> ModelGroupVO.ModelSimpleVO.builder()
                        .id(m.getId())
                        .modelName(m.getModelName())
                        .modelCode(m.getModelCode())
                        .build())
                .collect(Collectors.toList());

        return ModelGroupVO.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .modelIds(modelIds)
                .models(models)
                .status(group.getStatus())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new BusinessException("模型ID列表序列化失败");
        }
    }

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析模型ID列表失败，原始值：{}", json);
            return new ArrayList<>();
        }
    }
}
