package com.school.teacherEval.service;

import com.school.teacherEval.entity.EvaluationPeriod;
import com.school.teacherEval.repository.EvaluationPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationPeriodService {
    
    private final EvaluationPeriodRepository periodRepository;
    
    public List<EvaluationPeriod> getAllPeriods() {
        return periodRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public EvaluationPeriod getPeriodById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考核周期不存在"));
    }
    
    public EvaluationPeriod getActivePeriod() {
        return periodRepository.findByStatus(EvaluationPeriod.Status.active)
                .orElse(null);
    }
    
    @Transactional
    public EvaluationPeriod createPeriod(EvaluationPeriod period) {
        return periodRepository.save(period);
    }
    
    @Transactional
    public EvaluationPeriod updatePeriod(Long id, EvaluationPeriod period) {
        EvaluationPeriod existing = periodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考核周期不存在"));
        
        if (period.getName() != null) {
            existing.setName(period.getName());
        }
        if (period.getStartDate() != null) {
            existing.setStartDate(period.getStartDate());
        }
        if (period.getEndDate() != null) {
            existing.setEndDate(period.getEndDate());
        }
        if (period.getDescription() != null) {
            existing.setDescription(period.getDescription());
        }
        if (period.getStatus() != null) {
            existing.setStatus(period.getStatus());
        }
        
        return periodRepository.save(existing);
    }
    
    @Transactional
    public void deletePeriod(Long id) {
        periodRepository.deleteById(id);
    }
}