package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentStatusVO {
    private String businessStatus;
    private String statusText;
    private List<String> availableActions;
}
