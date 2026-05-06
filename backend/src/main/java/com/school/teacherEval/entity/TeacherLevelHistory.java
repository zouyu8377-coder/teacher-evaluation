package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teacher_level_history", indexes = {
    @Index(name = "idx_teacher_id", columnList = "teacher_id")
})
public class TeacherLevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_level", nullable = false, length = 10)
    private TeacherLevel oldLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_level", nullable = false, length = 10)
    private TeacherLevel newLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 10)
    private ChangeType changeType;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ChangeType {
        AUTO, MANUAL
    }
}
