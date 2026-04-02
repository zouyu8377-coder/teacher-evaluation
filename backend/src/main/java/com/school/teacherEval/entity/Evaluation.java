package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "evaluations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"teacher_id", "period_id"})
})
public class Evaluation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "evaluator_id", nullable = false)
    private Long evaluatorId;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(name = "period_id", nullable = false)
    private Long periodId;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "attachments", length = 1000)
    private String attachments;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.draft;
    
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
    
    public enum Status {
        draft, submitted
    }
}