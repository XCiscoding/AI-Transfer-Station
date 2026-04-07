package com.aikey.service;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.model.ModelCreateRequest;
import com.aikey.dto.model.ModelVO;
import com.aikey.entity.Channel;
import com.aikey.entity.Model;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型管理服务类
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final ChannelRepository channelRepository;

    /**
     * 分页查询模型列表
     */
    @Transactional(readOnly = true)
    public PageResult<ModelVO> listModels(int page, int size, String keyword, String modelType, Long channelId) {
        Specification<Model> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.or(
                    cb.like(root.get("modelName"), "%" + keyword + "%"),
                    cb.like(root.get("modelCode"), "%" + keyword + "%")
                ));
            }
            if (StringUtils.hasText(modelType)) {
                predicates.add(cb.equal(root.get("modelType"), modelType));
            }
            if (channelId != null) {
                predicates.add(cb.equal(root.get("channel").get("id"), channelId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Model> pageResult = modelRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<ModelVO> records = pageResult.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.<ModelVO>builder()
                .records(records)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    /**
     * 创建模型
     */
    public ModelVO createModel(ModelCreateRequest request) {
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        Model model = Model.builder()
                .modelName(request.getModelName())
                .modelCode(request.getModelCode())
                .modelAlias(request.getModelAlias())
                .channel(channel)
                .modelType(request.getModelType())
                .capabilityTags(request.getCapabilityTags())
                .maxTokens(request.getMaxTokens())
                .inputPrice(request.getInputPrice())
                .outputPrice(request.getOutputPrice())
                .quotaWeight(request.getQuotaWeight())
                .remark(request.getRemark())
                .status(1)
                .deleted(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toVO(modelRepository.save(model));
    }

    /**
     * 切换模型状态
     */
    public void toggleStatus(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("模型不存在"));
        model.setStatus(model.getStatus() == 1 ? 0 : 1);
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);
    }

    /**
     * 删除模型（逻辑删除）
     */
    public void deleteModel(Long id) {
        Model model = modelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("模型不存在"));
        model.setDeleted(1);
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);
    }

    private ModelVO toVO(Model model) {
        return ModelVO.builder()
                .id(model.getId())
                .modelName(model.getModelName())
                .modelCode(model.getModelCode())
                .modelAlias(model.getModelAlias())
                .channelId(model.getChannel().getId())
                .channelName(model.getChannel().getChannelName())
                .modelType(model.getModelType())
                .capabilityTags(model.getCapabilityTags())
                .maxTokens(model.getMaxTokens())
                .inputPrice(model.getInputPrice())
                .outputPrice(model.getOutputPrice())
                .quotaWeight(model.getQuotaWeight())
                .status(model.getStatus())
                .remark(model.getRemark())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
