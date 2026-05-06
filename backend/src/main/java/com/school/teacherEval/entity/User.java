package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "real_name", length = 50)
    private String realName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    
    @Column(length = 100)
    private String department;
    
    @Column(length = 500)
    private String avatar;
    
    @Column(nullable = false)
    private Integer status = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "teacher_level", nullable = false, length = 10)
    private TeacherLevel teacherLevel = TeacherLevel.NONE;

    @Column(name = "level_changed_at")
    private LocalDateTime levelChangedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Role {
        teacher, evaluator, admin
    }
}