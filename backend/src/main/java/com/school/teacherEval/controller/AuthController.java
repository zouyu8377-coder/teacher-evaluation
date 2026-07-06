package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.dto.ChangePasswordDTO;
import com.school.teacherEval.dto.LoginRequest;
import com.school.teacherEval.dto.LoginResponse;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.security.JwtUtil;
import com.school.teacherEval.security.TokenBlacklistService;
import com.school.teacherEval.service.UserService;
import com.school.teacherEval.vo.CurrentUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            return ApiResponse.error(401, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "登录失败，请稍后重试");
        }
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户")
    public ApiResponse<CurrentUserVO> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getCurrentUser(username);
        CurrentUserVO vo = new CurrentUserVO(
            user.getId(),
            user.getUsername(),
            user.getRealName(),
            user.getRole().name(),
            user.getDepartment()
        );
        return ApiResponse.success(vo);
    }

    @PutMapping("/password")
    @Operation(summary = "修改当前用户密码")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        userService.changeOwnPassword(auth.getName(), request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功", null);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Date expiration = jwtUtil.getExpirationDateFromToken(token);
                tokenBlacklistService.blacklistToken(token, expiration);
            } catch (Exception e) {
                // Token 解析失败时忽略，直接返回退出成功
            }
        }
        return ApiResponse.success("退出成功", null);
    }
}
