package com.aikey.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 *
 * <p>提供系统健康状态检查接口</p>
 */
@RestController
public class HealthController {

    /**
     * 健康检查接口
     * 返回服务运行状态
     *
     * @return 健康状态信息
     */
    @GetMapping("/actuator/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }
}
