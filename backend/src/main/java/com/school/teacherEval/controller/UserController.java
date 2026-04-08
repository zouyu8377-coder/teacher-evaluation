package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    @Operation(summary = "用户列表")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<User> userPage = userService.getUsers(role, keyword, page, size);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", userPage.getContent().stream().map(this::toVO).collect(Collectors.toList()));
        data.put("total", userPage.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        
        return ApiResponse.success(data);
    }
    
    @GetMapping("/teachers")
    @Operation(summary = "教师列表")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<Map<String, Object>>> getTeachers() {
        List<User> teachers = userService.getTeachers();
        List<Map<String, Object>> result = teachers.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("realName", t.getRealName());
                    map.put("department", t.getDepartment());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @GetMapping("/evaluators")
    @Operation(summary = "评分人列表")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<List<Map<String, Object>>> getEvaluators() {
        List<User> evaluators = userService.getEvaluators();
        List<Map<String, Object>> result = evaluators.stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("realName", e.getRealName());
                    map.put("department", e.getDepartment());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ApiResponse.success(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        return ApiResponse.success(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("删除成功", null);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public ApiResponse<Map<String, Object>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ApiResponse.success(toVO(user));
    }
    
    private Map<String, Object> toVO(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("role", user.getRole().name());
        map.put("department", user.getDepartment());
        map.put("status", user.getStatus());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }
}