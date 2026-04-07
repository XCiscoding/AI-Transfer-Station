package com.aikey;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 临时密码验证工具类
 * 用于调试登录500错误
 */
public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "admin123";
        String hashFromDataSql = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";

        System.out.println("=== BCrypt密码验证 ===");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("data.sql中的哈希: " + hashFromDataSql);
        System.out.println("验证结果: " + encoder.matches(rawPassword, hashFromDataSql));

        // 生成新的正确哈希
        String newHash = encoder.encode(rawPassword);
        System.out.println("\n新生成的哈希: " + newHash);
        System.out.println("新哈希验证: " + encoder.matches(rawPassword, newHash));
    }
}
