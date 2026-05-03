package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.dto.UserCreateDTO;
import com.school.teacherEval.dto.UserUpdateDTO;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.UserService;
import com.school.teacherEval.vo.PageVO;
import com.school.teacherEval.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ApiResponse<PageVO<UserVO>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<User> userPage = userService.getUsers(role, keyword, page, size);
        List<UserVO> records = userPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        PageVO<UserVO> data = new PageVO<>(
                records,
                userPage.getTotalElements(),
                page,
                size
        );
        return ApiResponse.success(data);
    }

    @GetMapping("/teachers")
    @Operation(summary = "教师列表")
    @PreAuthorize("hasAnyRole('admin', 'evaluator')")
    public ApiResponse<List<UserVO>> getTeachers() {
        List<User> teachers = userService.getTeachers();
        List<UserVO> result = teachers.stream()
                .map(t -> new UserVO(
                        t.getId(),
                        t.getUsername(),
                        t.getRealName(),
                        null,
                        t.getDepartment(),
                        null,
                        null
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/evaluators")
    @Operation(summary = "评分人列表")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<List<UserVO>> getEvaluators() {
        List<User> evaluators = userService.getEvaluators();
        List<UserVO> result = evaluators.stream()
                .map(e -> new UserVO(
                        e.getId(),
                        e.getUsername(),
                        e.getRealName(),
                        null,
                        e.getDepartment(),
                        null,
                        null
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
    
    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<User> createUser(@RequestBody UserCreateDTO dto) {
        User created = userService.createUser(dto);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        User updated = userService.updateUser(id, dto);
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
    public ApiResponse<UserVO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ApiResponse.success(toVO(user));
    }

    private UserVO toVO(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole().name(),
                user.getDepartment(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}