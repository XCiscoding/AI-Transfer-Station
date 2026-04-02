# MVP专属任务清单

## 概述

**文档版本：** V1.0
**创建日期：** 2026-04-02
**基于决策：** 项目总经理MVP决策 - 选择M001/M002/M003三个模块作为MVP核心
**总任务数：** 13个（从原58个任务精简）
**总预估工时：** 41小时
**目标：** 实现系统基础认证、渠道管理、APIKey管理三大核心功能，形成可演示的最小可行产品

---

## MVP范围定义

### 包含模块
- **M001 - 系统基础模块（登录认证）**
- **M002 - 渠道管理**
- **M003 - APIKey管理**

### 排除功能（后续迭代）
- M004 调度与代理网关
- M005 额度与计费
- M006 监控与日志
- M007 用户与团队
- M008 开放平台
- M009 部署与运维
- M010 Auto模式
- RBAC权限模型（TASK-M001-008）
- 系统设置接口（TASK-M001-009）

### MVP核心价值主张
1. **用户可登录系统** - 基于JWT的安全认证
2. **管理员可管理渠道** - 添加/编辑/测试外部AI厂商渠道
3. **管理员可管理API Key** - 加密存储真实Key，生成和管理虚拟Key
4. **完整的前后端交互** - 从数据库到API到前端页面的完整链路

---

## 任务清单详情

### 阶段一：基础设施搭建（2个任务, 5小时）

---

#### TASK-M001-002: 后端Spring Boot项目搭建

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-002 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 3小时 |
| **依赖** | 无 |
| **执行Agent** | 后端开发工程师 |

**描述：**
搭建Spring Boot后端项目基础结构，配置Maven依赖、项目分层结构、基础配置文件。

**验收标准：**
- [ ] 创建Spring Boot 3.x项目（Java 17）
- [ ] 配置pom.xml包含所有必要依赖
- [ ] 建立标准分层结构（controller/service/repository/entity/dto/config/security/util）
- [ ] 配置application.yml（数据库连接、Redis、日志级别等）
- [ ] 项目可成功启动并访问健康检查端点 `/actuator/health`

**技术要点：**

**1. Maven依赖清单（pom.xml关键依赖）：**
```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- MySQL驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- JWT支持 (JJWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Swagger/OpenAPI文档 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**2. 项目包结构：**
```
com.aikey/
├── AiKeyManagementApplication.java      # 启动类
├── config/
│   ├── SecurityConfig.java              # 安全配置（初始放行所有请求）
│   ├── SwaggerConfig.java               # Swagger配置
│   └── RedisConfig.java                 # Redis配置
├── controller/
│   └── HealthController.java            # 健康检查控制器
├── entity/
├── repository/
├── service/
├── dto/
│   └── common/
│       ├── Result.java                  # 统一返回结果
│       └── PageResult.java              # 分页结果
├── security/
│   └── JwtTokenProvider.java           # JWT工具类（占位）
├── util/
│   └── AesEncryptUtil.java             # AES加密工具类（占位）
└── exception/
    ├── GlobalExceptionHandler.java     # 全局异常处理
    └── BusinessException.java         # 业务异常类
```

**3. application.yml配置模板：**
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_key_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # 使用已有schema，不自动建表
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

# JWT配置
jwt:
  secret: your-secret-key-must-be-at-least-256-bits-long
  expiration: 7200000            # 2小时（毫秒）

# AES加密密钥（需32字符）
aes:
  secret-key: your-aes-256-bit-secret-key-32chars!

# 日志配置
logging:
  level:
    com.aikey: debug
    org.springframework.security: debug
```

**4. 关键文件实现指导：**

**Result.java（统一返回结果）：**
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

**GlobalExceptionHandler.java（全局异常处理）：**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统内部错误");
    }
}
```

**预期输出：**
- 可编译运行的Spring Boot项目
- 访问 `http://localhost:8080/actuator/health` 返回UP状态
- 访问 `http://localhost:8080/swagger-ui.html` 可查看Swagger文档页面

---

#### TASK-M001-003: 前端Vue3项目搭建

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-003 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 3小时 |
| **依赖** | 无 |
| **执行Agent** | 前端开发工程师 |

**描述：**
搭建Vue 3前端项目基础结构，配置Vite构建工具、Element Plus UI库、路由、状态管理、API封装等。

**验收标准：**
- [ ] 创建Vue 3 + Vite项目
- [ ] 配置package.json包含所有必要依赖
- [ ] 集成Element Plus UI库
- [ ] 配置Vue Router路由（含登录页和主布局）
- [ ] 配置Pinia状态管理（用户状态store）
- [ ] 封装Axios HTTP客户端（拦截器、错误处理）
- [ ] 创建基础布局组件（侧边栏+顶栏+内容区）
- [ ] 项目可成功启动并显示登录页

**技术要点：**

**1. package.json关键依赖：**
```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "element-plus": "^2.6.0",
    "axios": "^1.7.0",
    "@element-plus/icons-vue": "^2.3.1"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.4.0",
    "sass": "^1.71.0",
    "unplugin-auto-import": "^0.17.0",
    "unplugin-vue-components": "^0.27.0"
  }
}
```

**2. 项目目录结构：**
```
frontend/src/
├── App.vue
├── main.js
├── api/
│   ├── index.js                    # Axios实例配置
│   ├── auth.js                     # 认证相关API
│   ├── channel.js                  # 渠道管理API
│   └── apikey.js                   # APIKey管理API
├── assets/
│   └── styles/
│       └── variables.scss          # 全局样式变量
├── components/
│   └── common/                     # 公共组件
├── layout/
│   ├── MainLayout.vue              # 主布局（侧边栏+顶栏+内容区）
│   ├── Sidebar.vue                 # 侧边栏导航
│   └── HeaderBar.vue               # 顶部导航栏
├── router/
│   └── index.js                    # 路由配置
├── store/
│   ├── index.js                    # Pinia入口
│   └── modules/
│       └── user.js                 # 用户状态管理
├── utils/
│   ├── request.js                  # Axios封装
│   └── auth.js                     # Token存储工具
└── views/
    ├── login/
    │   └── LoginView.vue           # 登录页面
    └── dashboard/
        └── DashboardView.vue       # 仪表盘首页（占位）
```

**3. Axios封装（utils/request.js）：**
```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 注入Token
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一错误处理
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // Token过期或无效
      if (res.code === 401) {
        userStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

**4. 路由配置示例（router/index.js）：**
```javascript
const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      // 后续添加渠道管理和APIKey管理路由
    ]
  }
]

// 路由守卫 - 未登录跳转登录页
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})
```

**5. Vite代理配置（vite.config.js）：**
```javascript
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

**预期输出：**
- 运行 `npm run dev` 可访问 `http://localhost:3000`
- 显示登录页面（用户名/密码输入框、登录按钮）
- 登录后可进入主布局页面（侧边栏+顶栏+内容区）

---

### 阶段二：用户认证模块（3个任务, 9小时）

---

#### TASK-M001-004: User/Role/Permission实体+Repository

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-004 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 2小时 |
| **依赖** | TASK-M001-002 |
| **执行Agent** | 后端开发工程师 |

**描述：**
创建用户、角色、权限实体类和对应的Repository接口，为认证授权模块提供数据访问基础。

**验收标准：**
- [ ] 创建User实体类（对应users表）
- [ ] 创建Role实体类（对应roles表）
- [ ] 创建Permission实体类（对应permissions表）
- [ ] 创建UserRole关联实体
- [ ] 创建RolePermission关联实体
- [ ] 创建对应的Repository接口（继承JpaRepository）
- [ ] 编写单元测试验证CRUD操作

**技术要点：**

**1. User实体类（entity/User.java）：**
```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;  // BCrypt加密

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String realName;

    @Column(length = 255)
    private String avatar;

    @Column(name = "status", nullable = false)
    private Integer status;  // 0-禁用，1-启用

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;  // 逻辑删除

    // 关联关系
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

**2. Role实体类（entity/Role.java）：**
```java
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @Column(length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    // 关联关系
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
```

**3. Repository接口（repository/UserRepository.java）：**
```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByStatusAndDeleted(Integer status, Integer deleted);
}
```

**4. 文件路径映射：**
- `backend/src/main/java/com/aikey/entity/User.java`
- `backend/src/main/java/com/aikey/entity/Role.java`
- `backend/src/main/java/com/aikey/entity/Permission.java`
- `backend/src/main/java/com/aikey/entity/UserRole.java`（可选，如果需要独立操作）
- `backend/src/main/java/com/aikey/repository/UserRepository.java`
- `backend/src/main/java/com/aikey/repository/RoleRepository.java`
- `backend/src/main/java/com/aikey/repository/PermissionRepository.java`

**预期输出：**
- 4个实体类完成映射到schema.sql中的表结构
- 3个Repository接口提供基本CRUD能力
- 单元测试验证实体映射正确性

---

#### TASK-M001-005: JWT认证+Spring Security

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-005 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 4小时 |
| **依赖** | TASK-M001-004 |
| **执行Agent** | 后端开发工程师 |

**描述：**
实现基于JWT的用户认证功能，包括Token生成、验证、刷新机制，以及Spring Security集成。

**验收标准：**
- [ ] 实现JWT工具类JwtTokenProvider（生成Token、验证Token、解析Token）
- [ ] 配置SecurityFilterChain过滤器链
- [ ] 实现JWT认证过滤器JwtAuthenticationFilter
- [ ] 实现UserDetailsService实现类（从数据库加载用户）
- [ ] 配置密码编码器BCryptPasswordEncoder
- [ ] 编写单元测试验证Token生成和解析

**技术要点：**

**1. JwtTokenProvider工具类（security/JwtTokenProvider.java）：**
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    // 生成Token
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // 从Token获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // 验证Token有效性
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**2. JWT认证过滤器（security/JwtAuthenticationFilter.java）：**
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**3. SecurityConfig配置（config/SecurityConfig.java）：**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT不需要）
            .csrf(csrf -> csrf.disable())
            // 配置Session策略（无状态JWT）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 放行公开接口
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**4. 自定义UserDetailsService（service/UserDetailsServiceImpl.java）：**
```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            getAuthorities(user.getRoles())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()))
                .collect(Collectors.toList());
    }
}
```

**5. 关键文件路径：**
- `backend/src/main/java/com/aikey/security/JwtTokenProvider.java`
- `backend/src/main/java/com/aikey/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/aikey/config/SecurityConfig.java`
- `backend/src/main/java/com/aikey/service/UserDetailsServiceImpl.java`

**预期输出：**
- JWT Token可正常生成和验证
- 登录接口返回的Token可用于后续请求认证
- 未携带Token或无效Token的请求返回401

---

#### TASK-M001-006: 登录接口实现

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-006 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 3小时 |
| **依赖** | TASK-M001-005 |
| **执行Agent** | 后端开发工程师 |

**描述：**
实现用户登录接口，包括账号密码登录、登录日志记录等功能。

**验收标准：**
- [ ] 实现登录接口 POST /api/v1/auth/login
- [ ] 实现DTO类（LoginRequest、LoginResponse）
- [ ] 实现AuthService业务逻辑（验证账号密码、生成Token、记录日志）
- [ ] 实现AuthController控制器
- [ ] 返回Token和用户基本信息
- [ ] 编写API文档注解

**技术要点：**

**1. DTO设计：**
```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}

// LoginResponse.java
@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
}
```

**2. AuthService实现（service/AuthService.java）：**
```java
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        // 1. 认证用户
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // 2. 生成JWT Token
        String token = tokenProvider.generateToken((UserDetails) authentication.getPrincipal());

        // 3. 查询用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 4. 更新最后登录时间和IP
        user.setLastLoginTime(LocalDateTime.now());
        // user.setLastLoginIp(clientIp); // 从请求中获取
        userRepository.save(user);

        // 5. 构建响应
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
```

**3. AuthController（controller/AuthController.java）：**
```java
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录获取JWT Token")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户信息")
    public Result<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // 返回当前用户信息
        UserInfoResponse userInfo = authService.getUserInfo(userDetails.getUsername());
        return Result.success(userInfo);
    }
}
```

**4. 关键文件路径：**
- `backend/src/main/java/com/aikey/dto/auth/LoginRequest.java`
- `backend/src/main/java/com/aikey/dto/auth/LoginResponse.java`
- `backend/src/main/java/com/aikey/dto/auth/UserInfoResponse.java`
- `backend/src/main/java/com/aikey/service/AuthService.java`
- `backend/src/main/java/com/aikey/controller/AuthController.java`

**预期输出：**
- POST /api/v1/auth/login 接口可用
- 正确账号密码返回Token和用户信息
- 错误账号密码返回401错误
- Swagger文档中可查看接口说明

---

#### TASK-M001-007: 登录前端页面

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M001-007 |
| **模块** | M001 - 系统基础模块 |
| **优先级** | MVP-P0 |
| **预估工时** | 3小时 |
| **依赖** | TASK-M001-003, TASK-M001-006 |
| **执行Agent** | 前端开发工程师 |

**描述：**
实现前端登录页面，包括登录表单、错误提示、登录成功跳转等功能。

**验收标准：**
- [ ] 创建登录页面组件 LoginView.vue
- [ ] 实现登录表单（用户名、密码输入框）
- [ ] 表单验证（非空校验）
- [ ] 调用登录API
- [ ] 成功后存储Token并跳转到控制台
- [ ] 错误提示显示
- [ ] 页面美观且符合Element Plus规范

**技术要点：**

**1. 登录页面组件（views/login/LoginView.vue）：**
```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2>AI调度中心 - 企业API Key管理系统</h2>
      </template>

      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(loginForm)
    // 存储Token和用户信息
    userStore.setToken(res.data.token)
    userStore.setUser(res.data)

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-card {
    width: 420px;

    h2 {
      text-align: center;
      color: #303133;
      margin-bottom: 20px;
    }
  }
}
</style>
```

**2. 用户状态管理（store/modules/user.js）：**
```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUser(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    setUser(res.data)
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    setToken,
    setUser,
    fetchUserInfo,
    logout
  }
})
```

**3. API封装（api/auth.js）：**
```javascript
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}
```

**4. 关键文件路径：**
- `frontend/src/views/login/LoginView.vue`
- `frontend/src/store/modules/user.js`
- `frontend/src/api/auth.js`
- `frontend/src/utils/auth.js`（可选，Token存储辅助函数）

**预期输出：**
- 登录页面美观可用
- 输入正确的账号密码可成功登录并跳转到主页
- 错误的账号密码显示错误提示
- Token正确存储在localStorage

---

### 阶段三：渠道管理模块（3个任务, 10小时）

---

#### TASK-M002-001: Channel/Model实体+Repository

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M002-001 |
| **模块** | M002 - 渠道管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 2小时 |
| **依赖** | TASK-M001-002（项目已搭建） |
| **执行Agent** | 后端开发工程师 |

**描述：**
创建渠道和模型实体类及对应的Repository接口。

**验收标准：**
- [ ] 创建Channel实体类（对应channels表）
- [ ] 创建Model实体类（对应models表）
- [ ] 创建对应的Repository接口
- [ ] 编写单元测试验证CRUD操作

**技术要点：**

**1. Channel实体类（entity/Channel.java）- 核心字段映射：**
```java
@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_name", nullable = false, length = 100)
    private String channelName;

    @Column(name = "channel_code", nullable = false, unique = true, length = 50)
    private String channelCode;

    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;  // openai, qwen, wenxin, etc.

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;  // AES加密后的API Key

    @Column(columnDefinition = "TEXT")
    private String models;  // JSON数组格式的模型列表

    @Column(nullable = false)
    private Integer weight;  // 权重0-100

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Integer status;  // 0-禁用，1-启用，2-维护中

    @Column(name = "health_status", nullable = false)
    private Integer healthStatus;

    // ... 其他字段参考schema.sql

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Model> modelSet = new HashSet<>();
}
```

**2. Model实体类（entity/Model.java）：**
```java
@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_code", nullable = false, length = 100)
    private String modelCode;

    @Column(name = "model_alias", length = 100)
    private String modelAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;  // chat, embedding, image

    // ... 其他字段参考schema.sql
}
```

**3. ChannelRepository自定义查询方法：**
```java
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByStatusAndDeletedOrderByPriorityDesc(Integer status, Integer deleted);

    List<Channel> findByChannelTypeAndStatusAndDeleted(String channelType, Integer status, Integer deleted);

    long countByStatusAndDeleted(Integer status, Integer deleted);
}
```

**4. 文件路径：**
- `backend/src/main/java/com/aikey/entity/Channel.java`
- `backend/src/main/java/com/aikey/entity/Model.java`
- `backend/src/main/java/com/aikey/repository/ChannelRepository.java`
- `backend/src/main/java/com/aikey/repository/ModelRepository.java`

**预期输出：**
- 渠道和模型实体完整映射到数据库表
- Repository支持基本的查询操作
- 单元测试验证实体关系正确性

---

#### TASK-M002-002: 渠道管理接口

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M002-002 |
| **模块** | M002 - 渠道管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 4小时 |
| **依赖** | TASK-M002-001 |
| **执行Agent** | 后端开发工程师 |

**描述：**
实现渠道管理接口，包括渠道的增删改查、连通性测试、状态管理等功能。

**验收标准：**
- [ ] 实现渠道列表查询接口（分页、筛选）
- [ ] 实现渠道创建接口
- [ ] 实现渠道更新接口
- [ ] 实现渠道删除接口（逻辑删除）
- [ ] 实现渠道启用/禁用切换接口
- [ ] 实现渠道连通性测试接口（异步）
- [ ] 编写Swagger API文档注解

**技术要点：**

**1. DTO设计：**
```java
// ChannelCreateRequest.java
@Data
public class ChannelCreateRequest {
    @NotBlank(message = "渠道名称不能为空")
    private String channelName;

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;  // openai, qwen, etc.

    @NotBlank(message = "Base URL不能为空")
    private String baseUrl;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;  // 明文，后端自动AES加密

    private Integer weight = 100;
    private Integer priority = 0;
    private String remark;
}

// ChannelUpdateRequest.java
@Data
public class ChannelUpdateRequest {
    private String channelName;
    private String baseUrl;
    private String apiKey;  // 如果更新则重新加密
    private Integer weight;
    private Integer priority;
    private String remark;
    private Integer status;
}

// ChannelVO.java - 列表展示用
@Data
@Builder
public class ChannelVO {
    private Long id;
    private String channelName;
    private String channelCode;
    private String channelType;
    private String baseUrl;
    private String apiKeyMask;  // 掩码显示 sk-***...***abc
    private Integer weight;
    private Integer priority;
    private Integer status;
    private Integer healthStatus;
    private LocalDateTime createdAt;
}
```

**2. ChannelService核心方法：**
```java
@Service
@Transactional
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    // 创建渠道
    public Channel createChannel(ChannelCreateRequest request) {
        // 检查编码唯一性
        if (channelRepository.existsByChannelCode(request.getChannelCode())) {
            throw new BusinessException("渠道编码已存在");
        }

        Channel channel = Channel.builder()
                .channelName(request.getChannelName())
                .channelCode(request.getChannelCode())
                .channelType(request.getChannelType())
                .baseUrl(request.getBaseUrl())
                .apiKeyEncrypted(aesEncryptUtil.encrypt(request.getApiKey(), aesSecretKey))
                .weight(request.getWeight())
                .priority(request.getPriority())
                .remark(request.getRemark())
                .status(1)  // 默认启用
                .deleted(0)
                .build();

        return channelRepository.save(channel);
    }

    // 分页查询
    public PageResult<ChannelVO> listChannels(int page, int size, String keyword, String channelType) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Channel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.or(
                    cb.like(root.get("channelName"), "%" + keyword + "%"),
                    cb.like(root.get("channelCode"), "%" + keyword + "%")
                ));
            }

            if (StringUtils.hasText(channelType)) {
                predicates.add(cb.equal(root.get("channelType"), channelType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Channel> pageResult = channelRepository.findAll(spec, pageable);

        List<ChannelVO> voList = pageResult.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.<ChannelVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    // 转换为VO（掩码显示API Key）
    private ChannelVO convertToVO(Channel channel) {
        String mask = maskApiKey(channel.getApiKeyEncrypted());
        return ChannelVO.builder()
                .id(channel.getId())
                .channelName(channel.getChannelName())
                .channelCode(channel.getChannelCode())
                .channelType(channel.getChannelType())
                .baseUrl(channel.getBaseUrl())
                .apiKeyMask(mask)
                .weight(channel.getWeight())
                .priority(channel.getPriority())
                .status(channel.getStatus())
                .healthStatus(channel.getHealthStatus())
                .createdAt(channel.getCreatedAt())
                .build();
    }

    // API Key掩码显示
    private String maskApiKey(String encryptedKey) {
        try {
            String originalKey = aesEncryptUtil.decrypt(encryptedKey, aesSecretKey);
            if (originalKey.length() > 10) {
                return originalKey.substring(0, 3) + "***...***" +
                       originalKey.substring(originalKey.length() - 3);
            }
            return "***";
        } catch (Exception e) {
            return "***";
        }
    }

    // 连通性测试（异步）
    @Async
    public CompletableFuture<Boolean> testConnection(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        // 解密API Key
        String apiKey = aesEncryptUtil.decrypt(channel.getApiKeyEncrypted(), aesSecretKey);

        // 发送测试请求到Base URL（例如 /models 接口）
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                channel.getBaseUrl() + "/models",
                HttpMethod.GET,
                entity,
                String.class
            );

            // 更新健康状态
            channel.setHealthStatus(response.getStatusCode().is2xxSuccessful() ? 1 : 0);
            channel.setHealthCheckTime(LocalDateTime.now());
            channelRepository.save(channel);

            return CompletableFuture.completedFuture(response.getStatusCode().is2xxSuccessful());
        } catch (Exception e) {
            channel.setHealthStatus(0);
            channel.setFailCount(channel.getFailCount() + 1);
            channelRepository.save(channel);
            return CompletableFuture.completedFuture(false);
        }
    }
}
```

**3. ChannelController：**
```java
@RestController
@RequestMapping("/api/v1/channels")
@Tag(name = "渠道管理", description = "AI厂商渠道的增删改查接口")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    @Operation(summary = "创建渠道")
    public Result<ChannelVO> create(@Valid @RequestBody ChannelCreateRequest request) {
        return Result.success(channelService.createChannel(request));
    }

    @GetMapping
    @Operation(summary = "渠道列表")
    public Result<PageResult<ChannelVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channelType) {
        return Result.success(channelService.listChannels(page, size, keyword, channelType));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新渠道")
    public Result<ChannelVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelUpdateRequest request) {
        return Result.success(channelService.updateChannel(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除渠道")
    public Result<Void> delete(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换渠道状态")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        channelService.toggleStatus(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试渠道连通性")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.success(channelService.testConnection(id).join());
    }
}
```

**4. 关键文件路径：**
- `backend/src/main/java/com/aikey/dto/channel/ChannelCreateRequest.java`
- `backend/src/main/java/com/aikey/dto/channel/ChannelUpdateRequest.java`
- `backend/src/main/java/com/aikey/dto/channel/ChannelVO.java`
- `backend/src/main/java/com/aikey/service/ChannelService.java`
- `backend/src/main/java/com/aikey/controller/ChannelController.java`

**预期输出：**
- 渠道CRUD接口全部可用
- API Key在数据库中以密文存储，列表中掩码显示
- 连通性测试可检测渠道是否可达
- Swagger文档完整清晰

---

#### TASK-M002-003: 渠道管理前端页面

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M002-003 |
| **模块** | M002 - 渠道管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 4小时 |
| **依赖** | TASK-M001-003, TASK-M002-002 |
| **执行Agent** | 前端开发工程师 |

**描述：**
实现前端渠道管理页面，包括渠道列表、创建/编辑表单、连通性测试、状态切换等功能。

**验收标准：**
- [ ] 创建渠道列表页面（表格展示、分页、搜索筛选）
- [ ] 创建渠道创建/编辑对话框（表单）
- [ ] 实现渠道类型下拉选择（OpenAI、通义、文心等）
- [ ] 实现连通性测试按钮及结果显示
- [ ] 实现渠道启用/禁用切换
- [ ] 实现渠道删除确认对话框
- [ ] 实现API Key掩码显示
- [ ] 表单验证

**技术要点：**

**1. 渠道列表页面（views/channel/ChannelList.vue）：**
```vue
<template>
  <div class="channel-container">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="名称/编码" clearable />
        </el-form-item>
        <el-form-item label="渠道类型">
          <el-select v-model="searchForm.channelType" placeholder="全部" clearable>
            <el-option label="OpenAI" value="openai" />
            <el-option label="通义千问" value="qwen" />
            <el-option label="文心一言" value="wenxin" />
            <el-option label="豆包" value="doubao" />
            <el-option label="Claude" value="claude" />
            <el-option label="Gemini" value="gemini" />
            <el-option label="DeepSeek" value="deepseek" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">搜索</el-button>
          <el-button icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-card class="table-card">
      <template #header>
        <div style="display: flex; justify-content: space-between;">
          <span>渠道列表</span>
          <el-button type="primary" icon="Plus" @click="handleAdd">新增渠道</el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="channelName" label="渠道名称" width="150" />
        <el-table-column prop="channelCode" label="渠道编码" width="120" />
        <el-table-column prop="channelType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ getChannelTypeName(row.channelType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="apiKeyMask" label="API Key" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleToggleStatus(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="healthStatus" label="健康状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.healthStatus === 1 ? 'success' : 'danger'">
              {{ row.healthStatus === 1 ? '健康' : '不健康' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
        style="margin-top: 16px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <ChannelDialog
      v-model:visible="dialogVisible"
      :channel-id="editChannelId"
      @success="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChannelList, deleteChannel, updateChannelStatus, testChannelConnection } from '@/api/channel'
import ChannelDialog from './components/ChannelDialog.vue'

// ... 数据和方法实现
</script>
```

**2. 渠道表单对话框（components/ChannelDialog.vue）：**
```vue
<template>
  <el-dialog
    :title="isEdit ? '编辑渠道' : '新增渠道'"
    :model-value="visible"
    width="600px"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="渠道名称" prop="channelName">
        <el-input v-model="form.channelName" placeholder="请输入渠道名称" />
      </el-form-item>

      <el-form-item label="渠道编码" prop="channelCode">
        <el-input v-model="form.channelCode" placeholder="请输入渠道编码（英文）" :disabled="isEdit" />
      </el-form-item>

      <el-form-item label="渠道类型" prop="channelType">
        <el-select v-model="form.channelType" placeholder="请选择渠道类型">
          <el-option label="OpenAI" value="openai" />
          <el-option label="通义千问" value="qwen" />
          <!-- ...其他选项 -->
        </el-select>
      </el-form-item>

      <el-form-item label="Base URL" prop="baseUrl">
        <el-input v-model="form.baseUrl" placeholder="例如: https://api.openai.com/v1" />
      </el-form-item>

      <el-form-item label="API Key" prop="apiKey">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          placeholder="请输入API Key"
        />
      </el-form-item>

      <el-form-item label="权重" prop="weight">
        <el-input-number v-model="form.weight" :min="0" :max="100" />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
```

**3. API封装（api/channel.js）：**
```javascript
import request from '@/utils/request'

// 获取渠道列表
export function getChannelList(params) {
  return request({
    url: '/channels',
    method: 'get',
    params
  })
}

// 创建渠道
export function createChannel(data) {
  return request({
    url: '/channels',
    method: 'post',
    data
  })
}

// 更新渠道
export function updateChannel(id, data) {
  return request({
    url: `/channels/${id}`,
    method: 'put',
    data
  })
}

// 删除渠道
export function deleteChannel(id) {
  return request({
    url: `/channels/${id}`,
    method: 'delete'
  })
}

// 切换状态
export function updateChannelStatus(id, status) {
  return request({
    url: `/channels/${id}/status`,
    method: 'put',
    data: { status }
  })
}

// 测试连通性
export function testChannelConnection(id) {
  return request({
    url: `/channels/${id}/test`,
    method: 'post'
  })
}
```

**4. 关键文件路径：**
- `frontend/src/views/channel/ChannelList.vue`
- `frontend/src/views/channel/components/ChannelDialog.vue`
- `frontend/src/api/channel.js`
- 在 `router/index.js` 中添加渠道管理路由

**预期输出：**
- 渠道列表页面可正常显示数据
- 新增/编辑对话框可用，表单验证生效
- 连通性测试按钮点击后有反馈
- 状态开关可切换
- 分页和搜索功能正常

---

### 阶段四：APIKey管理模块（5个任务, 17小时）

---

#### TASK-M003-001: RealKey实体(AES加密)+Repository

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M003-001 |
| **模块** | M003 - APIKey管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 2小时 |
| **依赖** | TASK-M001-002（项目已搭建） |
| **执行Agent** | 后端开发工程师 |

**描述：**
创建真实Key实体类及对应的Repository接口，实现AES-256加密存储。

**验收标准：**
- [ ] 创建RealKey实体类（对应real_keys表）
- [ ] 实现AesEncryptUtil工具类（AES-256加解密）
- [ ] 实现JPA AttributeConverter用于自动加解密
- [ ] 创建RealKeyRepository接口
- [ ] 编写单元测试验证加密解密功能

**技术要点：**

**1. AES加密工具类（util/AesEncryptUtil.java）：**
```java
@Component
public class AesEncryptUtil {

    @Value("${aes.secret-key}")
    private String secretKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * AES-GCM加密
     */
    public String encrypt(String plaintext, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合 IV + 密文（IV不需要保密但必须唯一）
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, encryptedWithIv, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES-GCM解密
     */
    public String decrypt(String ciphertext, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);

            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // 提取IV
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
}
```

**2. RealKey实体类（entity/RealKey.java）：**
```java
@Entity
@Table(name = "real_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @Column(name = "key_value_encrypted", columnDefinition = "TEXT", nullable = false)
    private String keyValueEncrypted;  // AES加密后的Key值

    @Column(name = "key_mask", nullable = false, length = 50)
    private String keyMask;  // 掩码值，如sk-***...***abc

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private Integer status;  // 0-禁用，1-启用

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount;

    // ... 其他字段参考schema.sql

    @Column(name = "deleted", nullable = false)
    private Integer deleted;
}
```

**3. RealKeyRepository：**
```java
public interface RealKeyRepository extends JpaRepository<RealKey, Long> {
    List<RealKey> findByChannelIdAndDeleted(Long channelId, Integer deleted);

    List<RealKey> findByStatusAndDeleted(Integer status, Integer deleted);

    long countByChannelIdAndStatusAndDeleted(Long channelId, Integer status, Integer deleted);
}
```

**4. 文件路径：**
- `backend/src/main/java/com/aikey/util/AesEncryptUtil.java`
- `backend/src/main/java/com/aikey/entity/RealKey.java`
- `backend/src/main/java/com/aikey/repository/RealKeyRepository.java`

**预期输出：**
- AES加密工具类可正常工作
- RealKey实体映射到real_keys表
- 单元测试验证加密解密的正确性和安全性

---

#### TASK-M003-002: 真实Key管理接口

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M003-002 |
| **模块** | M003 - APIKey管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 3小时 |
| **依赖** | TASK-M003-001 |
| **执行Agent** | 后端开发工程师 |

**描述：**
实现真实Key管理接口，包括Key的录入、列表展示（掩码）、批量导入、状态管理等功能。

**验收标准：**
- [ ] 实现真实Key录入接口（自动加密存储并生成掩码）
- [ ] 实现真实Key列表查询接口（掩码显示）
- [ ] 实现真实Key更新接口
- [ ] 实现真实Key启用/禁用接口
- [ ] 实现真实Key删除接口（逻辑删除）
- [ ] 编写API文档

**技术要点：**

**1. DTO设计：**
```java
// RealKeyCreateRequest.java
@Data
public class RealKeyCreateRequest {
    @NotBlank(message = "Key名称不能为空")
    private String keyName;

    @NotBlank(message = "Key值不能为空")
    private String keyValue;  // 明文，如sk-xxxxx

    @NotNull(message = "所属渠道不能为空")
    private Long channelId;

    private LocalDateTime expireTime;
    private String remark;
}

// RealKeyVO.java
@Data
@Builder
public class RealKeyVO {
    private Long id;
    private String keyName;
    private String keyMask;  // 掩码显示
    private Long channelId;
    private String channelName;
    private Integer status;
    private LocalDateTime expireTime;
    private Long usageCount;
    private LocalDateTime lastUsedTime;
    private LocalDateTime createdAt;
}
```

**2. RealKeyService核心逻辑：**
```java
@Service
@Transactional
public class RealKeyService {

    @Autowired
    private RealKeyRepository realKeyRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private AesEncryptUtil aesEncryptUtil;

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    // 创建真实Key
    public RealKey createRealKey(RealKeyCreateRequest request) {
        // 验证渠道存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        // 加密Key值
        String encryptedValue = aesEncryptUtil.encrypt(request.getKeyValue(), aesSecretKey);

        // 生成掩码
        String mask = generateMask(request.getKeyValue());

        RealKey realKey = RealKey.builder()
                .keyName(request.getKeyName())
                .keyValueEncrypted(encryptedValue)
                .keyMask(mask)
                .channel(channel)
                .status(1)
                .usageCount(0L)
                .deleted(0)
                .expireTime(request.getExpireTime())
                .remark(request.getRemark())
                .build();

        return realKeyRepository.save(realKey);
    }

    // 生成掩码（保留前3位和后3位）
    private String generateMask(String key) {
        if (key.length() > 10) {
            return key.substring(0, 3) + "***...***" + key.substring(key.length() - 3);
        }
        return "***";
    }

    // 分页查询
    public PageResult<RealKeyVO> listRealKeys(int page, int size, Long channelId, String keyword) {
        // 类似ChannelService的实现...
    }
}
```

**3. RealKeyController：**
```java
@RestController
@RequestMapping("/api/v1/real-keys")
@Tag(name = "真实Key管理", description = "真实API Key的管理接口")
@RequiredArgsConstructor
public class RealKeyController {

    private final RealKeyService realKeyService;

    @PostMapping
    @Operation(summary = "录入真实Key")
    public Result<RealKeyVO> create(@Valid @RequestBody RealKeyCreateRequest request) {
        return Result.success(realKeyService.createRealKey(request));
    }

    @GetMapping
    @Operation(summary = "真实Key列表")
    public Result<PageResult<RealKeyVO>> list(/* 参数 */) {
        return Result.success(realKeyService.listRealKeys(/* 参数 */));
    }

    // ... 其他接口
}
```

**4. 关键文件路径：**
- `backend/src/main/java/com/aikey/dto/realkey/RealKeyCreateRequest.java`
- `backend/src/main/java/com/aikey/dto/realkey/RealKeyVO.java`
- `backend/src/main/java/com/aikey/service/RealKeyService.java`
- `backend/src/main/java/com/aikey/controller/RealKeyController.java`

**预期输出：**
- 真实Key CRUD接口可用
- Key值在数据库中加密存储
- 列表中只显示掩码，不暴露明文

---

#### TASK-M003-003: VirtualKey实体+Repository

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M003-003 |
| **模块** | M003 - APIKey管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 2小时 |
| **依赖** | TASK-M001-002（项目已搭建） |
| **执行Agent** | 后端开发工程师 |

**描述：**
创建虚拟Key实体类及对应的Repository接口。

**验收标准：**
- [ ] 创建VirtualKey实体类（对应virtual_keys表）
- [ ] 创建VirtualKeyRepository接口
- [ ] 实现Key生成规则（sk-xxx格式）
- [ ] 编写单元测试验证CRUD操作

**技术要点：**

**1. VirtualKey实体类（entity/VirtualKey.java）：**
```java
@Entity
@Table(name = "virtual_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = "key_value")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @Column(name = "key_value", nullable = false, unique = true, length = 100)
    private String keyValue;  // sk-xxx格式，明文存储（本身就是虚拟的）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "allowed_models", columnDefinition = "TEXT")
    private String allowedModels;  // JSON数组，空表示不限制

    @Column(name = "quota_type", nullable = false, length = 20)
    private String quotaType;  // token, count, amount

    @Column(name = "quota_limit", nullable = false)
    private BigDecimal quotaLimit;

    @Column(name = "quota_used", nullable = false)
    private BigDecimal quotaUsed;

    @Column(name = "quota_remaining", nullable = false)
    private BigDecimal quotaRemaining;

    @Column(name = "rate_limit_qpm")
    private Integer rateLimitQpm;  // 每分钟限制

    @Column(name = "rate_limit_qpd")
    private Integer rateLimitQpd;  // 每日限制

    @Column(nullable = false)
    private Integer status;  // 0-禁用，1-启用

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    // ... 其他字段参考schema.sql

    @Column(name = "deleted", nullable = false)
    private Integer deleted;
}
```

**2. Key生成工具方法（可在VirtualKeyService中实现）：**
```java
// 生成虚拟Key值
private String generateKeyValue() {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return "sk-" + uuid.substring(0, 32);  // sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
}
```

**3. VirtualKeyRepository：**
```java
public interface VirtualKeyRepository extends JpaRepository<VirtualKey, Long> {
    Optional<VirtualKey> findByKeyValue(String keyValue);

    List<VirtualKey> findByUserIdAndDeleted(Long userId, Integer deleted);

    List<VirtualKey> findByStatusAndDeleted(Integer status, Integer deleted);

    Page<VirtualKey> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    boolean existsByKeyValue(String keyValue);
}
```

**4. 文件路径：**
- `backend/src/main/java/com/aikey/entity/VirtualKey.java`
- `backend/src/main/java/com/aikey/repository/VirtualKeyRepository.java`

**预期输出：**
- VirtualKey实体完整映射到virtual_keys表
- Repository支持按用户、按Key值查询
- Key生成规则符合sk-xxx格式要求

---

#### TASK-M003-004: 虚拟Key管理接口

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M003-004 |
| **模块** | M003 - APIKey管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 4小时 |
| **依赖** | TASK-M003-003 |
| **执行Agent** | 后端开发工程师 |

**描述：**
实现虚拟Key管理接口，包括Key的生成、绑定、状态维护、生命周期管理等功能。

**验收标准：**
- [ ] 实现虚拟Key生成接口（绑定用户、模型、额度、限速配置）
- [ ] 实现虚拟Key列表查询接口（支持分页、筛选）
- [ ] 实现虚拟Key更新接口
- [ ] 实现虚拟Key启用/禁用接口
- [ ] 实现虚拟Key删除接口
- [ ] 实现虚拟Key刷新接口（重新生成Key值）
- [ ] 编写API文档

**技术要点：**

**1. DTO设计：**
```java
// VirtualKeyCreateRequest.java
@Data
public class VirtualKeyCreateRequest {
    @NotBlank(message = "Key名称不能为空")
    private String keyName;

    @NotNull(message = "所属用户不能为空")
    private Long userId;

    private Long teamId;
    private Long projectId;

    private String allowedModels;  // JSON数组格式

    @NotBlank(message = "额度类型不能为空")
    private String quotaType;  // token, count, amount

    @NotNull(message = "额度上限不能为空")
    private BigDecimal quotaLimit;

    private Integer rateLimitQpm = 60;  // 默认每分钟60次
    private Integer rateLimitQpd = 0;   // 0表示不限制

    private LocalDateTime expireTime;
    private String remark;
}

// VirtualKeyVO.java
@Data
@Builder
public class VirtualKeyVO {
    private Long id;
    private String keyName;
    private String keyValue;  // 完整显示（因为是虚拟Key，不是真实的）
    private Long userId;
    private String userName;
    private String allowedModels;
    private String quotaType;
    private BigDecimal quotaLimit;
    private BigDecimal quotaUsed;
    private BigDecimal quotaRemaining;
    private Integer rateLimitQpm;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
}
```

**2. VirtualKeyService核心方法：**
```java
@Service
@Transactional
public class VirtualKeyService {

    @Autowired
    private VirtualKeyRepository virtualKeyRepository;

    @Autowired
    private UserRepository userRepository;

    // 生成虚拟Key
    public VirtualKey createVirtualKey(VirtualKeyCreateRequest request) {
        // 验证用户存在
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 生成唯一的Key值
        String keyValue;
        do {
            keyValue = generateKeyValue();
        } while (virtualKeyRepository.existsByKeyValue(keyValue));

        VirtualKey virtualKey = VirtualKey.builder()
                .keyName(request.getKeyName())
                .keyValue(keyValue)
                .user(user)
                .teamId(request.getTeamId())
                .projectId(request.getProjectId())
                .allowedModels(request.getAllowedModels())
                .quotaType(request.getQuotaType())
                .quotaLimit(request.getQuotaLimit())
                .quotaUsed(BigDecimal.ZERO)
                .quotaRemaining(request.getQuotaLimit())
                .rateLimitQpm(request.getRateLimitQpm())
                .rateLimitQpd(request.getRateLimitQpd())
                .status(1)
                .expireTime(request.getExpireTime())
                .deleted(0)
                .build();

        return virtualKeyRepository.save(virtualKey);
    }

    // 刷新Key值（重新生成）
    public VirtualKey refreshKey(Long id) {
        VirtualKey existingKey = virtualKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

        String newKeyValue;
        do {
            newKeyValue = generateKeyValue();
        } while (virtualKeyRepository.existsByKeyValue(newKeyValue));

        existingKey.setKeyValue(newKeyValue);
        return virtualKeyRepository.save(existingKey);
    }

    // 生成Key值
    private String generateKeyValue() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "sk-" + uuid.substring(0, 32);
    }
}
```

**3. VirtualKeyController：**
```java
@RestController
@RequestMapping("/api/v1/virtual-keys")
@Tag(name = "虚拟Key管理", description = "虚拟API Key的管理接口")
@RequiredArgsConstructor
public class VirtualKeyController {

    private final VirtualKeyService virtualKeyService;

    @PostMapping
    @Operation(summary = "生成虚拟Key")
    public Result<VirtualKeyVO> create(@Valid @RequestBody VirtualKeyCreateRequest request) {
        return Result.success(virtualKeyService.createVirtualKey(request));
    }

    @GetMapping
    @Operation(summary = "虚拟Key列表")
    public Result<PageResult<VirtualKeyVO>> list(/* 参数 */) {
        return Result.success(virtualKeyService.listVirtualKeys(/* 参数 */));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新虚拟Key")
    public Result<VirtualKeyVO> update(@PathVariable Long id, @RequestBody /* */) {
        // ...
    }

    @PutMapping("/{id}/refresh")
    @Operation(summary = "刷新虚拟Key值")
    public Result<VirtualKeyVO> refresh(@PathVariable Long id) {
        return Result.success(virtualKeyService.refreshKey(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换虚拟Key状态")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody /* */) {
        // ...
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除虚拟Key")
    public Result<Void> delete(@PathVariable Long id) {
        // ...
    }
}
```

**4. 关键文件路径：**
- `backend/src/main/java/com/aikey/dto/virtualkey/VirtualKeyCreateRequest.java`
- `backend/src/main/java/com/aikey/dto/virtualkey/VirtualKeyVO.java`
- `backend/src/main/java/com/aikey/service/VirtualKeyService.java`
- `backend/src/main/java/com/aikey/controller/VirtualKeyController.java`

**预期输出：**
- 虚拟Key CRUD接口全部可用
- 生成的Key符合sk-xxx格式且唯一
- 刷新接口可重新生成Key值
- 额度和限速配置正确保存

---

#### TASK-M003-005: APIKey管理前端页面

| 属性 | 值 |
|------|-----|
| **任务ID** | TASK-M003-005 |
| **模块** | M003 - APIKey管理 |
| **优先级** | MVP-P0 |
| **预估工时** | 5小时 |
| **依赖** | TASK-M001-003, TASK-M003-002, TASK-M003-004 |
| **执行Agent** | 前端开发工程师 |

**描述：**
实现前端APIKey管理页面，包括真实Key管理和虚拟Key管理两个子页面。

**验收标准：**
- [ ] 创建APIKey管理主页面（Tab切换：真实Key / 虚拟Key）
- [ ] 创建真实Key管理子页面（列表、录入、状态切换）
- [ ] 创建虚拟Key管理子页面（列表、生成、编辑、刷新、状态切换）
- [ ] 实现Key掩码显示（真实Key）和完整显示（虚拟Key）
- [ ] 实现表单验证
- [ ] 实现权限控制（管理员可见所有功能）

**技术要点：**

**1. APIKey管理主页面（views/apikey/ApikeyManage.vue）：**
```vue
<template>
  <div class="apikey-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 真实Key管理Tab -->
      <el-tab-pane label="真实Key管理" name="realkey">
        <RealKeyList />
      </el-tab-pane>

      <!-- 虚拟Key管理Tab -->
      <el-tab-pane label="虚拟Key管理" name="virtualkey">
        <VirtualKeyList />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RealKeyList from './components/RealKeyList.vue'
import VirtualKeyList from './components/VirtualKeyList.vue'

const activeTab = ref('realkey')
</script>
```

**2. 真实Key列表组件（components/RealKeyList.vue）：**
- 参考渠道列表页面结构
- 特殊点：
  - Key列显示掩码值
  - 录入对话框需要选择所属渠道（下拉选择）
  - 不需要"刷新"按钮（真实Key不可刷新）

**3. 虚拟Key列表组件（components/VirtualKeyList.vue）：**
- 参考渠道列表页面结构
- 特殊点：
  - Key列完整显示（可复制）
  - 有"刷新Key"按钮（重新生成Key值）
  - 生成对话框需要选择所属用户（下拉选择）
  - 显示额度使用情况（进度条）

**4. API封装（api/apikey.js）：**
```javascript
import request from '@/utils/request'

// ===== 真实Key接口 =====
export function getRealKeyList(params) {
  return request({ url: '/real-keys', method: 'get', params })
}

export function createRealKey(data) {
  return request({ url: '/real-keys', method: 'post', data })
}

export function updateRealKeyStatus(id, status) {
  return request({ url: `/real-keys/${id}/status`, method: 'put', data: { status } })
}

export function deleteRealKey(id) {
  return request({ url: `/real-keys/${id}`, method: 'delete' })
}

// ===== 虚拟Key接口 =====
export function getVirtualKeyList(params) {
  return request({ url: '/virtual-keys', method: 'get', params })
}

export function createVirtualKey(data) {
  return request({ url: '/virtual-keys', method: 'post', data })
}

export function updateVirtualKey(id, data) {
  return request({ url: `/virtual-keys/${id}`, method: 'put', data })
}

export function refreshVirtualKey(id) {
  return request({ url: `/virtual-keys/${id}/refresh`, method: 'put' })
}

export function updateVirtualKeyStatus(id, status) {
  return request({ url: `/virtual-keys/${id}/status`, method: 'put', data: { status } })
}

export function deleteVirtualKey(id) {
  return request({ url: `/virtual-keys/${id}`, method: 'delete' })
}
```

**5. 关键文件路径：**
- `frontend/src/views/apikey/ApikeyManage.vue`
- `frontend/src/views/apikey/components/RealKeyList.vue`
- `frontend/src/views/apikey/components/VirtualKeyList.vue`
- `frontend/src/views/apikey/components/RealKeyDialog.vue`
- `frontend/src/views/apikey/components/VirtualKeyDialog.vue`
- `frontend/src/api/apikey.js`
- 在 `router/index.js` 中添加APIKey管理路由

**预期输出：**
- APIKey管理页面可正常切换两个Tab
- 真实Key列表显示掩码，可录入新Key
- 虚拟Key列表显示完整Key值，可生成、刷新、编辑
- 所有表单验证生效
- 页面布局美观，用户体验良好

---

## 执行序列建议

### 推荐执行顺序（考虑依赖关系）

```
第1步: TASK-M001-002 (后端项目搭建) [3h]
   ↓ 并行
第2步: TASK-M001-003 (前端项目搭建) [3h]
   ↓
第3步: TASK-M001-004 (User/Role/Permission实体) [2h] ← 依赖M001-002
   ↓
第4步: TASK-M001-005 (JWT认证+Security) [4h] ← 依赖M001-004
   ↓
第5步: TASK-M001-006 (登录接口) [3h] ← 依赖M001-005
   ↓ 并行
第6a步: TASK-M001-007 (登录前端页面) [3h] ← 依赖M001-003, M001-006
第6b步: TASK-M002-001 (Channel/Model实体) [2h] ← 依赖M001-002
   ↓
第7步: TASK-M002-002 (渠道管理接口) [4h] ← 依赖M002-001
   ↓
第8步: TASK-M002-003 (渠道管理前端页面) [4h] ← 依赖M001-003, M002-002
   ↓
第9步: TASK-M003-001 (RealKey实体+AES) [2h] ← 依赖M001-002
   ↓ 并行
第10a步: TASK-M003-003 (VirtualKey实体) [2h] ← 依赖M001-002
第10b步: TASK-M003-002 (真实Key管理接口) [3h] ← 依赖M003-001
   ↓
第11步: TASK-M003-004 (虚拟Key管理接口) [4h] ← 依赖M003-003
   ↓
第12步: TASK-M003-005 (APIKey管理前端页面) [5h] ← 依赖M001-003, M003-002, M003-004
```

### 时间线估算

| 天数 | 任务 | 累计工时 |
|------|------|----------|
| Day 1 | M001-002, M001-003（并行） | 6h |
| Day 2 | M001-004 | 8h |
| Day 3 | M001-005 | 12h |
| Day 4 | M001-006, M001-007（并行） | 18h |
| Day 5 | M002-001, M002-002 | 24h |
| Day 6 | M002-003 | 28h |
| Day 7 | M003-001, M003-003（并行） | 32h |
| Day 8 | M003-002, M003-004 | 39h |
| Day 9 | M003-005 | 44h（含缓冲） |

**总计：约9个工作日（41工时 + 缓冲）**

---

## Agent分配矩阵

| 任务ID | 任务名称 | 推荐Agent | 工时 |
|--------|----------|-----------|------|
| TASK-M001-002 | 后端Spring Boot项目搭建 | 后端开发工程师 | 3h |
| TASK-M001-003 | 前端Vue3项目搭建 | 前端开发工程师 | 3h |
| TASK-M001-004 | User/Role/Permission实体+Repository | 后端开发工程师 | 2h |
| TASK-M001-005 | JWT认证+Spring Security | 后端开发工程师 | 4h |
| TASK-M001-006 | 登录接口实现 | 后端开发工程师 | 3h |
| TASK-M001-007 | 登录前端页面 | 前端开发工程师 | 3h |
| TASK-M002-001 | Channel/Model实体+Repository | 后端开发工程师 | 2h |
| TASK-M002-002 | 渠道管理接口 | 后端开发工程师 | 4h |
| TASK-M002-003 | 渠道管理前端页面 | 前端开发工程师 | 4h |
| TASK-M003-001 | RealKey实体(AES加密)+Repository | 后端开发工程师 | 2h |
| TASK-M003-002 | 真实Key管理接口 | 后端开发工程师 | 3h |
| TASK-M003-003 | VirtualKey实体+Repository | 后端开发工程师 | 2h |
| TASK-M003-004 | 虚拟Key管理接口 | 后端开发工程师 | 4h |
| TASK-M003-005 | APIKey管理前端页面 | 前端开发工程师 | 5h |
| **合计** | | | **41h** |

---

## 验收标准（DoD）

### MVP整体验收标准

**功能完整性：**
- [ ] 用户可通过账号密码登录系统
- [ ] 登录后可看到主界面（侧边栏+顶栏+内容区）
- [ ] 管理员可添加、编辑、删除、禁用/启用渠道
- [ ] 渠道列表支持搜索、分页、类型筛选
- [ ] 渠道连通性测试功能可用
- [ ] 管理员可录入真实API Key（加密存储）
- [ ] 管理员可查看真实Key列表（掩码显示）
- [ ] 管理员可生成虚拟API Key（sk-xxx格式）
- [ ] 虚拟Key支持配置额度和限速
- [ ] 虚拟Key支持刷新、启用/禁用

**质量标准：**
- [ ] 所有API接口有Swagger文档
- [ ] 前端表单验证完善
- [ ] 错误提示友好明确
- [ ] 代码遵循项目规范
- [ ] 无明显UI/UX问题

**安全标准：**
- [ ] 密码BCrypt加密存储
- [ ] 真实Key AES-256加密存储
- [ ] JWT Token认证有效
- [ ] 未登录无法访问受保护资源
- [ ] 敏感数据不在前端明文展示

**性能标准：**
- [ ] 页面加载时间 < 2秒
- [ ] API平均响应时间 < 500ms（本地环境）
- [ ] 无明显卡顿现象

---

## 风险与依赖

### 技术风险

| 风险项 | 概率 | 影响 | 缓解措施 |
|--------|------|------|----------|
| AES加密实现不当导致安全性不足 | 低 | 高 | 使用成熟的AES-GCM模式，编写充分测试 |
| JWT Token泄露风险 | 中 | 中 | 设置合理过期时间，后续增加黑名单机制 |
| 前后端联调问题 | 中 | 中 | 提前约定好接口格式，使用Mock数据并行开发 |
| Element Plus组件兼容性问题 | 低 | 低 | 使用稳定版本，查阅官方文档 |

### 外部依赖

- **MySQL 8.0+**: 必须预先安装并初始化schema.sql
- **Redis 6.0+**: 用于缓存和限流（MVP阶段主要用于Token存储）
- **Node.js 16+**: 前端构建环境
- **Java 17**: 后端运行环境
- **Maven 3.8+**: 后端构建工具

### 关键假设

1. 开发环境已安装MySQL、Redis、Node.js、Java 17、Maven
2. schema.sql已在MySQL中执行完毕，初始数据已插入
3. 前后端开发者熟悉各自技术栈的基本使用
4. 所有任务由同一开发者串行执行（或前后端可并行）

---

## 相关文档索引

- [项目总体设计](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/.trae/planning/summary.md)
- [原始58任务清单](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/.trae/planning/tasks.md)
- [数据库Schema](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/db/schema.sql)
- [工作流规范](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/.trae/planning/spec.md)

---

**文档版本：** V1.0
**创建日期：** 2026-04-02
**创建者：** 项目策划师
**状态：** 已完成，待项目总经理审核
