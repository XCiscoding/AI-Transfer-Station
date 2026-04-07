# ISSUE-007: 前端访问后端接口返回403 Forbidden

## 问题信息
- **问题ID**: ISSUE-007
- **所属模块**: 后端 - Spring Security配置
- **问题类型**: 配置缺陷
- **严重程度**: 🔴 高
- **发现时间**: 2026-04-04
- **修复时间**: 2026-04-04
- **当前状态**: ✅ 已解决-测试通过

## 问题描述
前端应用（运行在 http://localhost:5173）访问后端API（http://localhost:8080）时，浏览器显示"访问 localhost 的请求遭到拒绝"，HTTP ERROR 403。

## 复现步骤
1. 启动后端服务（端口8080）
2. 启动前端服务（端口5173）
3. 前端尝试调用后端API
4. 浏览器发送CORS预检请求（OPTIONS）
5. 后端返回403 Forbidden

## 预期结果
前端可以正常访问后端API，CORS预检请求返回200，并包含正确的CORS响应头。

## 实际结果
CORS预检请求返回403 Forbidden，前端无法访问后端接口。

## 根因分析
**SecurityConfig.java 缺少CORS跨域配置**

当前配置中：
- ✅ 禁用了CSRF
- ✅ 配置了JWT认证
- ✅ 配置了请求授权规则
- ❌ **完全没有CORS跨域配置**

Spring Security默认情况下会拒绝跨域请求，需要显式配置CORS支持。

## 修复方案

### 修改文件
- `backend/src/main/java/com/aikey/config/SecurityConfig.java`

### 修复内容
1. 添加CORS相关导入：
   - `org.springframework.web.cors.CorsConfiguration`
   - `org.springframework.web.cors.CorsConfigurationSource`
   - `org.springframework.web.cors.UrlBasedCorsConfigurationSource`

2. 在filterChain中启用CORS：
   ```java
   http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
   ```

3. 添加CORS配置Bean：
   ```java
   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
       CorsConfiguration configuration = new CorsConfiguration();
       configuration.setAllowedOrigins(Arrays.asList(
           "http://localhost:5173",
           "http://localhost:3000",
           "http://localhost:8081",
           "http://127.0.0.1:5173",
           "http://127.0.0.1:3000"
       ));
       configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
       configuration.setAllowedHeaders(Arrays.asList("*"));
       configuration.setAllowCredentials(true);
       configuration.setMaxAge(3600L);

       UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
       source.registerCorsConfiguration("/**", configuration);
       return source;
   }
   ```

## 验证结果

### 测试1: CORS预检请求
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/login" -Method OPTIONS -Headers @{
    "Origin"="http://localhost:5173"
    "Access-Control-Request-Method"="POST"
    "Access-Control-Request-Headers"="Content-Type"
}
```
**结果**: ✅ StatusCode: 200
- Access-Control-Allow-Origin: http://localhost:5173
- Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
- Access-Control-Allow-Headers: Content-Type

### 测试2: Swagger UI访问
```
GET http://localhost:8080/swagger-ui/index.html
```
**结果**: ✅ StatusCode: 200，页面正常显示

### 测试3: 编译验证
```bash
mvn clean compile
```
**结果**: ✅ BUILD SUCCESS

## 影响范围
- 前端所有页面可以正常访问后端API
- 支持多种前端开发服务器端口（5173, 3000, 8081）
- 支持本地和127.0.0.1两种地址格式

## 相关文档
- [Spring Security CORS文档](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [MDN CORS指南](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

## 经验总结
**教训**: 前后端分离项目必须配置CORS，Spring Security不会自动处理跨域请求。
**最佳实践**: 
1. 在SecurityConfig中显式启用cors()
2. 配置允许的源地址（开发环境放宽，生产环境严格限制）
3. 允许必要的HTTP方法和请求头
4. 设置合理的预检缓存时间

---
*问题单由Debug工程师于2026-04-04创建*
*修复验证完成，状态更新为已解决*
