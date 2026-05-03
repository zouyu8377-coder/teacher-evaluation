package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerStatVO {
    private Long id;
    private String realName;
    private long completedCount;
    private long totalRequired;
}
