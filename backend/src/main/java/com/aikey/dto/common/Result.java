package com.aikey.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回结果封装类
 *
 * <p>所有API接口的统一响应格式</p>
 *
 * @param <T> 数据类型泛型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 200: 成功
     * 其他: 业务错误码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（带数据）
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 统一结果对象
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 统一结果对象
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一结果对象
     */
    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    /**
     * 错误响应（默认500）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一结果对象
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
