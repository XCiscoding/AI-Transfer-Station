package com.aikey.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密工具类
 *
 * <p>使用AES/GCM/NoPadding模式进行加解密操作，
 * 用于敏感数据（如API Key）的加密存储</p>
 */
@Component
public class AesEncryptUtil {

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    /**
     * AES-GCM算法标识
     */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * GCM认证标签长度（位）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM初始化向量长度（字节）
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * AES加密
     *
     * @param plaintext 明文
     * @param key       加密密钥（32字符，AES-256）
     * @return Base64编码的密文（包含IV）
     */
    public String encrypt(String plaintext, String key) throws Exception {
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new java.security.SecureRandom().nextBytes(iv);

        // 创建密钥规范
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "AES");

        // 初始化加密器
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        // 加密
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 组合IV + 密文并Base64编码
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * AES解密
     *
     * @param ciphertext Base64编码的密文（包含IV）
     * @param key        解密密钥（32字符，AES-256）
     * @return 解密后的明文
     */
    public String decrypt(String ciphertext, String key) throws Exception {
        // Base64解码
        byte[] combined = Base64.getDecoder().decode(ciphertext);

        // 提取IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        // 提取密文
        byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // 创建密钥规范
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "AES");

        // 初始化解密器
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        // 解密
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
