package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationListVO {
    private List<EvaluationVO> evaluations;
    private int count;
    private BigDecimal averageScore;
}
