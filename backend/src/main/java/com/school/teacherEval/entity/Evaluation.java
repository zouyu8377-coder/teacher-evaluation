package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "evaluations")
public class Evaluation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    
    @Column(name = "evaluator_id")
    private Long evaluatorId;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "attachments", length = 1000)
    private String attachments;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.draft;
    
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
    
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;
    
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "is_passed")
    private Boolean isPassed;

    @Column(name = "exam_record_id")
    private Long examRecordId;
    
    @Column(name = "auto_score", precision = 5, scale = 2)
    private BigDecimal autoScore;
    
    @Column(name = "manual_adjust", precision = 5, scale = 2)
    private BigDecimal manualAdjust = BigDecimal.ZERO;
    
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