package com.school.teacherEval.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TeacherDashboardDTO {

    // 用户基本信息
    private UserInfo userInfo;

    // 当前级别信息
    private LevelInfo currentLevel;

    // 当前报考/进行中的考核
    private List<EnrollmentInfo> currentEnrollments;

    // 历史考核记录
    private List<HistoryRecord> historyRecords;

    // 待办事项
    private List<TodoItem> todoItems;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String department;
        private String role;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class LevelInfo {
        private String level;
        private String levelName;
        private Boolean hasPassed;
        private LocalDateTime passedAt;
        private String nextLevel;
        private Boolean canEnrollNext;
    }

    @Data
    public static class EnrollmentInfo {
        private Long enrollmentId;
        private Long activityId;
        private String activityName;
        private String level;
        private String levelName;
        private String status;
        private LocalDateTime enrolledAt;
        private LocalDateTime examStartTime;
        private LocalDateTime examEndTime;
        private LocalDateTime materialStartTime;
        private LocalDateTime materialEndTime;
        private Boolean hasExam;
        private Integer examDurationMinutes;
        private String examStatus; // not_started, in_progress, completed
        private Long examRecordId;
        private Boolean scorePublished;
        private BigDecimal finalScore;
        private Boolean isPassed;
        private Integer correctCount;
        private Integer wrongCount;
    }

    @Data
    public static class HistoryRecord {
        private Long activityId;
        private String activityName;
        private String level;
        private String levelName;
        private String status;
        private BigDecimal finalScore;
        private Boolean isPassed;
        private LocalDateTime enrolledAt;
        private LocalDateTime completedAt;
    }

    @Data
    public static class TodoItem {
        private String type; // exam, enrollment, evaluation
        private String title;
        private String description;
        private Long relatedId;
        private String actionUrl;
        private LocalDateTime deadline;
    }
}