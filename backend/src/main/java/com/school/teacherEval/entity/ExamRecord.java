package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exam_records", indexes = {
    @Index(name = "idx_teacher_activity", columnList = "teacher_id, activity_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class ExamRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "paper_id", nullable = false)
    private Long paperId;
    
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(columnDefinition = "JSON")
    private String answers;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal score;
    
    @Column(name = "auto_score", precision = 5, scale = 2)
    private BigDecimal autoScore;
    
    @Column(name = "manual_adjust", precision = 5, scale = 2)
    private BigDecimal manualAdjust = BigDecimal.ZERO;
    
    @Column(name = "correct_count")
    private Integer correctCount = 0;
    
    @Column(name = "wrong_count")
    private Integer wrongCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.not_started;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Status {
        not_started, in_progress, not_submitted, submitted
    }

    /**
     * 判断考试是否已提交
     */
    public Boolean getIsSubmitted() {
        return this.status == Status.submitted;
    }

    /**
     * 获取更新时间，优先返回提交时间，否则返回创建时间
     */
    public LocalDateTime getUpdatedAt() {
        return this.submittedAt != null ? this.submittedAt : this.createdAt;
    }
}