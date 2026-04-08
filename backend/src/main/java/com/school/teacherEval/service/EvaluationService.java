package com.school.teacherEval.service;

import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    
    public Page<Evaluation> getEvaluations(Long activityId, Long teacherId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (activityId != null && teacherId != null) {
            return evaluationRepository.findByActivityId(activityId, pageable);
        }
        if (activityId != null) {
            return evaluationRepository.findByActivityId(activityId, pageable);
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
    
    public List<Evaluation> getTeacherActivityEvaluations(Long teacherId, Long activityId) {
        return evaluationRepository.findByTeacherIdAndActivityId(teacherId, activityId);
    }
    
    public List<Evaluation> getTeacherPublishedEvaluations(Long teacherId) {
        return evaluationRepository.findByTeacherIdAndIsPublished(teacherId);
    }
    
    public List<Evaluation> getEvaluatorEvaluations(Long evaluatorId) {
        return evaluationRepository.findByEvaluatorId(evaluatorId);
    }
    
    public List<Evaluation> getActivityTeacherEvaluations(Long activityId, Long teacherId) {
        return evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
    }
    
    @Transactional
    public Evaluation createOrUpdateEvaluation(Long evaluatorId, Long teacherId, Long activityId,
                                                BigDecimal score, String comment) {
        userRepository.findById(evaluatorId).orElseThrow(() -> new RuntimeException("考核员不存在"));
        userRepository.findById(teacherId).orElseThrow(() -> new RuntimeException("教师不存在"));
        
        Evaluation evaluation = evaluationRepository
                .findByTeacherIdAndActivityIdAndEvaluatorId(teacherId, activityId, evaluatorId)
                .orElse(new Evaluation());
        
        evaluation.setActivityId(activityId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setTeacherId(teacherId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setStatus(Evaluation.Status.submitted);
        
        return evaluationRepository.save(evaluation);
    }
    
    public long countByActivityId(Long activityId) {
        return evaluationRepository.countByActivityId(activityId);
    }
    
    public long countByStatus(Evaluation.Status status) {
        return evaluationRepository.countByStatus(status);
    }
    
    @Transactional
    public int publishScores(Long activityId, Long teacherId) {
        List<Evaluation> evaluations;
        if (teacherId != null) {
            evaluations = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        } else {
            evaluations = evaluationRepository.findByActivityId(activityId);
        }
        
        int count = 0;
        for (Evaluation eval : evaluations) {
            if (eval.getScore() != null && eval.getStatus() == Evaluation.Status.submitted) {
                List<Evaluation> allEvals = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
                BigDecimal avgScore = calculateAverageScore(allEvals);
                
                eval.setFinalScore(avgScore);
                eval.setIsPublished(true);
                eval.setIsLocked(true);
                evaluationRepository.save(eval);
                count++;
            }
        }
        return count;
    }
    
    public BigDecimal calculateAverageScore(List<Evaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        
        for (Evaluation eval : evaluations) {
            if (eval.getScore() != null) {
                sum = sum.add(eval.getScore());
                count++;
            }
        }
        
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
    
    public EvaluationSummary getActivitySummary(Long activityId, Long teacherId) {
        List<Evaluation> evaluations;
        if (teacherId != null) {
            evaluations = evaluationRepository.findByActivityIdAndTeacherId(activityId, teacherId);
        } else {
            evaluations = evaluationRepository.findByActivityId(activityId);
        }
        
        EvaluationSummary summary = new EvaluationSummary();
        summary.setTotalEvaluations(evaluations.size());
        
        if (!evaluations.isEmpty()) {
            BigDecimal avgScore = calculateAverageScore(evaluations);
            summary.setAverageScore(avgScore);
            summary.setEvaluations(evaluations);
        }
        
        return summary;
    }
    
    public static class EvaluationSummary {
        private int totalEvaluations;
        private BigDecimal averageScore;
        private List<Evaluation> evaluations;
        
        public int getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(int totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public BigDecimal getAverageScore() { return averageScore; }
        public void setAverageScore(BigDecimal averageScore) { this.averageScore = averageScore; }
        public List<Evaluation> getEvaluations() { return evaluations; }
        public void setEvaluations(List<Evaluation> evaluations) { this.evaluations = evaluations; }
    }
}