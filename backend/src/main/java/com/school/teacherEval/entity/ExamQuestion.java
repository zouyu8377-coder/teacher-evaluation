package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exam_questions", indexes = {
    @Index(name = "idx_type", columnList = "question_type"),
    @Index(name = "idx_status", columnList = "status")
})
public class ExamQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;
    
    @Column(columnDefinition = "JSON", nullable = false)
    private String options;
    
    @Column(name = "correct_answer", length = 20, nullable = false)
    private String correctAnswer;
    
    @Column(nullable = false)
    private Integer score = 5;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
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
    
    public enum QuestionType {
        single, multiple
    }
}