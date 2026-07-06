package com.school.teacherEval.service;

import com.school.teacherEval.dto.LoginRequest;
import com.school.teacherEval.dto.LoginResponse;
import com.school.teacherEval.dto.UserCreateDTO;
import com.school.teacherEval.dto.UserUpdateDTO;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.UserRepository;
import com.school.teacherEval.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户登录失败 - 密码错误: {}", request.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("用户登录失败 - 账号已被禁用: {}", request.getUsername());
            throw new BusinessException("账号已被禁用");
        }
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(LoginResponse.UserVO.from(user));
        return response;
    }
    
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    public Page<User> getUsers(String role, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (role != null && !role.isEmpty()) {
            User.Role userRole = User.Role.valueOf(role);
            if (keyword != null && !keyword.isEmpty()) {
                return userRepository.findByRoleAndRealNameContaining(userRole, keyword, pageable);
            }
            return userRepository.findByRole(userRole, pageable);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            return userRepository.findByRealNameContaining(keyword, pageable);
        }
        
        return userRepository.findAll(pageable);
    }
    
    @Transactional
    public User createUser(UserCreateDTO dto) {
        String username = normalizeUsername(dto.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setDepartment(dto.getDepartment());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        log.info("创建用户: {}, 角色: {}", user.getUsername(), user.getRole());
        return userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new BusinessException("新密码长度需为6-64位");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UserUpdateDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (dto.getRealName() != null) {
            existingUser.setRealName(dto.getRealName());
        }
        if (dto.getDepartment() != null) {
            existingUser.setDepartment(dto.getDepartment());
        }
        if (dto.getRole() != null) {
            existingUser.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            existingUser.setStatus(dto.getStatus());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return userRepository.save(existingUser);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public List<User> getTeachers() {
        return userRepository.findByRole(User.Role.teacher);
    }
    
    public List<User> getEvaluators() {
        return userRepository.findByRole(User.Role.evaluator);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("用户名需为3-50位，仅支持字母、数字、下划线、短横线和点");
        }
        return normalized;
    }
}
