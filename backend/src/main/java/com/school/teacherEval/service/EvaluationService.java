package com.school.teacherEval.service;

import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    
    public Page<Evaluation> getEvaluations(Long periodId, Long teacherId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (periodId != null && teacherId != null) {
            return evaluationRepository.findByTeacherIdAndPeriodId(teacherId, periodId, pageable);
        }
        if (periodId != null) {
            return evaluationRepository.findByPeriodId(periodId, pageable);
        }
        if (teacherId != null) {
            return evaluationRepository.findByTeacherId(teacherId, pageable);
        }
        
        return evaluationRepository.findAll(pageable);
    }
    
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分记录不存在"));
    }
    
    public List<Evaluation> getTeacherEvaluations(Long teacherId) {
        return evaluationRepository.findByTeacherId(teacherId);
    }
    
    public Evaluation getTeacherPeriodEvaluation(Long teacherId, Long periodId) {
        return evaluationRepository.findByTeacherIdAndPeriodId(teacherId, periodId)
                .orElse(null);
    }
    
    @Transactional
    public Evaluation createOrUpdateEvaluation(Long evaluatorId, Long teacherId, Long periodId,
                                                java.math.BigDecimal score, String comment) {
        // 验证考核员和教师存在
        userRepository.findById(evaluatorId).orElseThrow(() -> new RuntimeException("考核员不存在"));
        userRepository.findById(teacherId).orElseThrow(() -> new RuntimeException("教师不存在"));
        
        // 检查是否已存在评分记录
        Evaluation evaluation = evaluationRepository.findByTeacherIdAndPeriodId(teacherId, periodId)
                .orElse(new Evaluation());
        
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setTeacherId(teacherId);
        evaluation.setPeriodId(periodId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setStatus(Evaluation.Status.submitted);
        
        return evaluationRepository.save(evaluation);
    }
    
    public long countByPeriodId(Long periodId) {
        return evaluationRepository.countByPeriodId(periodId);
    }
    
    public long countByStatus(Evaluation.Status status) {
        return evaluationRepository.countByStatus(status);
    }
}