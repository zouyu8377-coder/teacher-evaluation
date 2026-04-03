package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "period_enrollments")
public class PeriodEnrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "period_id", nullable = false)
    private Long periodId;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.enrolled;
    
    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }
    
    public enum Status {
        enrolled, removed
    }
}