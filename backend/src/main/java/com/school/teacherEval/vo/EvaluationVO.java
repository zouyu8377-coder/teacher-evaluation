package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationVO {
    private Long id;
    private Long activityId;
    private Long evaluatorId;
    private Long teacherId;
    private BigDecimal score;
    private java.math.BigDecimal finalScore;
    private String comment;
    private String status;
    private Boolean isPublished;
    private Boolean isLocked;
    private Boolean isPassed;
    private LocalDateTime createdAt;
    private String evaluatorName;
    private String teacherName;
}
