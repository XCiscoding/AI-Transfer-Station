package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限实体类
 */
@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "permission_name", length = 100, nullable = false)
    private String permissionName;

    @Column(name = "permission_code", length = 100, nullable = false, unique = true)
    private String permissionCode;

    @Column(name = "resource_type", length = 50, nullable = false)
    private String resourceType;

    @Column(name = "resource_path", length = 255)
    private String resourcePath;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted;

    /**
     * 权限与角色多对多关系（由Role端维护）
     */
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;
}
