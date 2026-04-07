package com.aikey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * <p>每次请求时从HTTP头提取并验证JWT Token，
 * 将认证信息设置到Spring Security上下文中</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * 过滤器核心逻辑：从请求头提取JWT Token并完成认证
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头提取JWT Token
            String jwt = getJwtFromRequest(request);

            // 2. 如果存在Token且当前SecurityContext未认证，则进行认证
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 3. 验证Token有效性并解析用户名
                String username = jwtTokenProvider.getUsernameFromToken(jwt);

                if (StringUtils.hasText(username)) {
                    // 4. 通过userDetailsService加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 5. 创建认证Token并设置到SecurityContext
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("用户 {} 认证成功, URI: {}", username, request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            log.error("无法设置用户认证: {}", ex.getMessage());
        }

        // 6. 无论成功失败都继续执行后续过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Bearer Token
     *
     * @param request HTTP请求
     * @return JWT Token字符串（不含"Bearer "前缀），如果不存在则返回null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 去掉"Bearer "前缀
        }

        return null;
    }

    /**
     * 判断当前请求是否应该跳过JWT认证过滤
     *
     * <p>只对完全公开的认证端点（如登录、注册）跳过JWT验证，
     * 其他需要认证的端点（如/me获取当前用户信息）必须经过JWT验证。</p>
     * <p>注意：/v1/路径由SecurityConfig.gatewayFilterChain独立处理，此处也需跳过以避免冲突。</p>
     *
     * @param request HTTP请求
     * @return true表示跳过过滤，false表示执行过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 精确匹配公开认证端点，避免过度匹配导致受保护接口被跳过JWT验证（ISSUE-010修复）
        return path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.startsWith("/v1/");
    }
}
