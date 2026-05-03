package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSummaryVO {
    private int totalEvaluations;
    private BigDecimal averageScore;
    private List<EvaluationVO> evaluations;
}
