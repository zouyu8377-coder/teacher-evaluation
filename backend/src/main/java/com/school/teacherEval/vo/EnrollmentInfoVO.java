package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentInfoVO {
    private Long activityId;
    private String activityName;
    private String level;
    private Boolean hasExam;
    private Integer maxParticipants;
    private long enrolledCount;
    private int remaining;
    private LocalDateTime enrollmentStart;
    private LocalDateTime enrollmentEnd;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer reviewerCount;
    private LocalDateTime enrolledAt;
    private String enrollmentStatus;
    private Long examRecordId;
    private BigDecimal examScore;
    private String examStatus;
    private LocalDateTime examSubmittedAt;
    private Long documentId;
    private String documentTitle;
    private String documentFileName;
    private Long documentFileSize;
    private LocalDateTime documentCreatedAt;
    private Boolean scorePublished;
    private BigDecimal finalScore;
    private Boolean isPassed;
    private String comment;
    private String businessStatus;
    private String statusText;
    private List<String> availableActions;
}
