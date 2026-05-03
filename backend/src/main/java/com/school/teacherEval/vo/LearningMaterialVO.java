package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningMaterialVO {
    private Long id;
    private Long activityId;
    private String title;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private String creatorName;
    private String activityName;
}
