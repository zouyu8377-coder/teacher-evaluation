package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.dto.TeacherDashboardDTO;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.TeacherDashboardService;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/dashboard")
@RequiredArgsConstructor
@Tag(name = "教师个人首页")
@PreAuthorize("hasAnyRole('teacher', 'admin')") // 允许教师和管理员访问
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "获取教师首页数据")
    public ApiResponse<TeacherDashboardDTO> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getCurrentUser(username);
        TeacherDashboardDTO dashboard = teacherDashboardService.getDashboard(user.getId());
        return ApiResponse.success(dashboard);
    }
}