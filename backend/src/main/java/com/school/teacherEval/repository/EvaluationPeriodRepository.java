package com.school.teacherEval.repository;

import com.school.teacherEval.entity.EvaluationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationPeriodRepository extends JpaRepository<EvaluationPeriod, Long> {
    
    List<EvaluationPeriod> findAllByOrderByCreatedAtDesc();
    
    Optional<EvaluationPeriod> findByStatus(EvaluationPeriod.Status status);
}