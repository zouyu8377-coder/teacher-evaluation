package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewProgressVO {
    private int enrolledCount;
    private Integer reviewerCount;
    private List<ReviewerStatVO> reviewerStats;
    private long totalCompleted;
    private long totalRequired;
    private String reviewStatus;
    private Boolean scoresPublished;
}
