package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyEnrollmentVO {
    private Long id;
    private Long activityId;
    private LocalDateTime enrolledAt;
    private String activityName;
    private String level;
    private Boolean hasExam;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime examStart;
    private LocalDateTime examEnd;
    private LocalDateTime materialStart;
    private LocalDateTime materialEnd;
    private Long examRecordId;
    private java.math.BigDecimal examScore;
    private String examStatus;
    private LocalDateTime examSubmittedAt;
    private Long documentId;
    private Boolean scorePublished;
    private java.math.BigDecimal finalScore;
    private Boolean isPassed;
    private String comment;
}
