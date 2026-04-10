package com.aikey.controller;

import com.aikey.dto.common.Result;
import com.aikey.dto.user.UserCreateRequest;
import com.aikey.dto.user.UserCreateResponse;
import com.aikey.repository.UserRepository;
import com.aikey.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户列表查询接口")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "用户列表")
    public Result<List<UserSimpleVO>> list(@RequestParam(required = false) String keyword) {
        List<UserSimpleVO> users = userRepository.findByStatusAndDeleted(1, 0).stream()
                .filter(user -> keyword == null || keyword.isBlank() || user.getUsername().contains(keyword))
                .map(user -> UserSimpleVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .build())
                .toList();
        return Result.success(users);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public Result<UserCreateResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    @Data
    @Builder
    private static class UserSimpleVO {
        private Long id;
        private String username;
    }
}
