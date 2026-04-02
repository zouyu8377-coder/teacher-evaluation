package com.school.teacherEval.service;

import com.school.teacherEval.dto.LoginRequest;
import com.school.teacherEval.dto.LoginResponse;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.UserRepository;
import com.school.teacherEval.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
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
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (user.getRealName() != null) {
            existingUser.setRealName(user.getRealName());
        }
        if (user.getDepartment() != null) {
            existingUser.setDepartment(user.getDepartment());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
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
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}