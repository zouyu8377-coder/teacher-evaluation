package com.school.teacherEval.repository;

import com.school.teacherEval.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    Page<User> findByRole(User.Role role, Pageable pageable);
    
    Page<User> findByRoleAndRealNameContaining(User.Role role, String keyword, Pageable pageable);
    
    List<User> findByRole(User.Role role);
    
    Page<User> findByRealNameContaining(String keyword, Pageable pageable);
}