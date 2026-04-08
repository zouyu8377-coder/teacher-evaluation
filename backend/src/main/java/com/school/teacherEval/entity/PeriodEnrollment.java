package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "period_enrollments", indexes = {
    @Index(name = "idx_activity_id", columnList = "activity_id"),
    @Index(name = "idx_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_activity_teacher", columnList = "activity_id, teacher_id", unique = true)
})
public class PeriodEnrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.enrolled;
    
    public enum Status {
        enrolled, removed
    }
    
    @PrePersist
    protected void onCreate() {
        if (enrolledAt == null) {
            enrolledAt = LocalDateTime.now();
        }
    }
}