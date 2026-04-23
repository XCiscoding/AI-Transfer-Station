package com.aikey.exception;

import com.aikey.dto.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * <p>统一处理应用中抛出的各类异常，
 * 返回标准的Result格式响应</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.aikey.controller")
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常对象
     * @return 错误结果
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());
        // 根据业务错误码返回对应的HTTP状态码
        HttpStatus status = HttpStatus.OK;
        if (e.getCode() == 400) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e.getCode() == 401) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (e.getCode() == 403) {
            status = HttpStatus.FORBIDDEN;
        } else if (e.getCode() == 404) {
            status = HttpStatus.NOT_FOUND;
        }
        return new ResponseEntity<>(Result.error(e.getCode(), e.getMessage()), status);
    }

    /**
     * 处理方法级权限异常，避免 @PreAuthorize 拒绝被兜底包装成 500。
     *
     * @param e 权限拒绝异常
     * @return 403错误结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return new ResponseEntity<>(Result.error(403, "权限不足"), HttpStatus.FORBIDDEN);
    }

    /**
     * 处理认证异常（登录失败、密码错误等）
     *
     * @param e 认证异常对象
     * @return 401错误结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        // 区分不同的认证异常类型
        String message = "认证失败";
        if (e instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        } else if (e.getMessage() != null && e.getMessage().contains("用户不存在")) {
            message = "用户不存在";
        }
        return Result.error(401, message);
    }

    /**
     * 处理BadCredentialsException（错误的凭证）
     *
     * @param e 错误凭证异常
     * @return 401错误结果
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("凭证错误: {}", e.getMessage());
        return Result.error(401, "用户名或密码错误");
    }

    /**
     * 处理参数校验异常
     *
     * @param e 参数校验异常对象
     * @return 400错误结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数校验失败: {}", errors);
        return Result.error(400, "请求参数校验失败");
    }

    /**
     * 处理其他未捕获的异常
     *
     * @e 异常对象
     * @return 500错误结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception e) {
        log.error("系统内部错误", e);

        // 智能判断：如果异常消息包含认证相关信息，返回401而不是500
        String message = e.getMessage();
        if (message != null && (message.contains("用户名或密码错误") || message.contains("用户不存在"))) {
            log.warn("检测到认证相关异常，返回401而非500");
            return new ResponseEntity<>(
                Result.error(401, message.replace("系统内部错误 - RuntimeException: ", "")),
                HttpStatus.UNAUTHORIZED
            );
        }

        // TODO: 修复完成后删除详细信息，只返回"系统内部错误"
        return new ResponseEntity<>(
            Result.error(500, "系统内部错误"),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
