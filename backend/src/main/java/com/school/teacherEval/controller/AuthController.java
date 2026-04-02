package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.dto.LoginRequest;
import com.school.teacherEval.dto.LoginResponse;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.security.JwtUtil;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }
    
    @GetMapping("/current")
    @Operation(summary = "获取当前用户")
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getCurrentUser(username);
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole().name());
        data.put("department", user.getDepartment());
        
        return ApiResponse.success(data);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出成功", null);
    }
}