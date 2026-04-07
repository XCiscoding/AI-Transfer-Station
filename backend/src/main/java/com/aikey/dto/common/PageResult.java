package com.aikey.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果DTO
 *
 * <p>用于封装分页查询的结果数据</p>
 *
 * @param <T> 记录类型泛型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据记录列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从1开始）
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;
}
