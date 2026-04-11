package com.aikey.config;

import com.aikey.gateway.auth.VirtualKeyAuthFilter;
import com.aikey.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置类
 *
 * <p>配置JWT无状态认证机制</p>
 * <p>启用方法级安全注解（@PreAuthorize/@PostAuthorize）</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final VirtualKeyAuthFilter virtualKeyAuthFilter;

    /**
     * 网关代理API安全过滤链（Order 1，优先匹配）
     *
     * <p>匹配 /v1/** 路径，使用虚拟Key鉴权（sk-xxx），不走JWT。
     * 鉴权由VirtualKeyAuthFilter全权处理，Spring Security层面permitAll。</p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain gatewayFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/v1/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(virtualKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 管理API安全过滤链（Order 2）
     *
     * <p>匹配其他所有路径，使用JWT认证。</p>
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 启用CORS跨域支持
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. 禁用CSRF（前后端分离架构，使用JWT Token认证）
            .csrf(csrf -> csrf.disable())

            // 3. 设置Session策略为STATELESS（无状态，基于JWT）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 放行认证相关接口
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users").authenticated()

                // 放行Swagger UI文档接口
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()

                // 放行Actuator健康检查端点
                .requestMatchers("/actuator/**").permitAll()

                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )

            // 5. 在UsernamePasswordAuthenticationFilter之前添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 Bean
     * 使用BCrypt算法进行密码加密
     *
     * @return PasswordEncoder BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器 Bean
     * 用于登录认证流程
     *
     * @param authConfig 认证配置
     * @return AuthenticationManager 认证管理器
     * @throws Exception 异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS跨域配置
     * 允许前端应用访问后端API
     *
     * @return CorsConfigurationSource CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的前端地址
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite默认端口
            "http://localhost:5174",  // Vite备选端口
            "http://localhost:5175",  // Vite备选端口
            "http://localhost:3000",  // React默认端口
            "http://localhost:8081",  // 其他可能端口
            "http://localhost:4173",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://127.0.0.1:5175",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:4173",
            "http://111.230.113.110:8083"  // 云端生产地址
        ));
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带凭证（cookies等）
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
