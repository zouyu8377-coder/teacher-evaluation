package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    @Column(name = "enrollment_start", nullable = false)
    private LocalDateTime enrollmentStart;

    @Column(name = "enrollment_end", nullable = false)
    private LocalDateTime enrollmentEnd;

    @Column(name = "exam_start")
    private LocalDateTime examStart;

    @Column(name = "exam_end")
    private LocalDateTime examEnd;

    @Column(name = "material_start")
    private LocalDateTime materialStart;

    @Column(name = "material_end")
    private LocalDateTime materialEnd;

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
        C(0, "初级(C级)", null),
        B2(1, "中级(B2级)", List.of(C)),
        B1(1, "中级(B1级)", List.of(C)),
        A2(2, "高级(A2级)", List.of(B2, B1)),
        A1(2, "高级(A1级)", List.of(B2, B1));

        private final int tier; // 层级：0=C, 1=B, 2=A
        private final String displayName;
        private final List<Level> prevLevels; // 前置级别列表

        Level(int tier, String displayName, List<Level> prevLevels) {
            this.tier = tier;
            this.displayName = displayName;
            this.prevLevels = prevLevels != null ? prevLevels : new ArrayList<>();
        }

        public int getTier() {
            return tier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<Level> getPrevLevels() {
            return prevLevels;
        }

        /**
         * 判断是否与另一个级别为并列级别（同层级）
         */
        public boolean isParallelWith(Level other) {
            return this != other && this.tier == other.tier;
        }

        /**
         * 获取同一层级的所有并列级别
         */
        public List<Level> getParallelLevels() {
            List<Level> parallels = new ArrayList<>();
            for (Level level : Level.values()) {
                if (level.isParallelWith(this)) {
                    parallels.add(level);
                }
            }
            return parallels;
        }

        /**
         * 获取所有可以报考的下一级别
         */
        public static List<Level> getNextLevels(Level currentLevel) {
            List<Level> nextLevels = new ArrayList<>();
            for (Level level : Level.values()) {
                if (level.tier == currentLevel.tier + 1) {
                    nextLevels.add(level);
                }
            }
            return nextLevels;
        }

        /**
         * 判断通过某个级别后是否可以报考目标级别
         * @param passedLevel 已通过的级别
         * @param targetLevel 目标级别
         */
        public static boolean canProgressTo(Level passedLevel, Level targetLevel) {
            if (passedLevel == null || targetLevel == null) {
                return false;
            }
            // 如果目标级别没有前置要求（只有C级没有前置要求）
            if (targetLevel.prevLevels.isEmpty()) {
                return true;
            }
            // 检查passedLevel是否是targetLevel的前置级别之一
            return targetLevel.prevLevels.contains(passedLevel);
        }

        /**
         * 获取级别的排序值，数值越高级别越高
         */
        public int getOrder() {
            return this.tier;
        }

        /**
         * 获取目标级别的前置级别（取第一个前置级别即可）
         */
        public static Level getPrevLevel(Level targetLevel) {
            if (targetLevel == null || targetLevel.prevLevels.isEmpty()) {
                return null;
            }
            return targetLevel.prevLevels.get(0);
        }
    }
    
    public enum Status {
        draft, active, closed
    }
}