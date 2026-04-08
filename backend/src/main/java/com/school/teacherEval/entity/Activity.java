package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_status", columnList = "status")
})
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Level level;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "max_participants")
    private Integer maxParticipants;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.draft;
    
    @Column(name = "enrollment_start")
    private LocalDateTime enrollmentStart;
    
    @Column(name = "enrollment_end")
    private LocalDateTime enrollmentEnd;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "reviewer_count", nullable = false)
    private Integer reviewerCount = 2;
    
    @Column(name = "reviewer_ids", columnDefinition = "JSON")
    private String reviewerIds;
    
    @Column(name = "exam_paper_id")
    private Long examPaperId;
    
    @Column(name = "has_exam")
    private Boolean hasExam = false;
    
    @Column(name = "exam_duration_minutes")
    private Integer examDurationMinutes = 60;
    
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
    
    public enum Level {
        C(0), B2(1), B1(2), A2(3), A1(4);
        
        private final int order;
        
        Level(int order) {
            this.order = order;
        }
        
        public int getOrder() {
            return order;
        }
        
        public static Level getPrevLevel(Level level) {
            return switch (level) {
                case C -> null;
                case B2 -> C;
                case B1 -> B2;
                case A2 -> B1;
                case A1 -> A2;
            };
        }
    }
    
    public enum Status {
        draft, active, closed
    }
}