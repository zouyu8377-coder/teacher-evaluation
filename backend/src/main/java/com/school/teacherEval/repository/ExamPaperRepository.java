package com.school.teacherEval.repository;

import com.school.teacherEval.entity.ExamPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    
    List<ExamPaper> findByActivityId(Long activityId);
    
    Page<ExamPaper> findByActivityId(Long activityId, Pageable pageable);
    
    List<ExamPaper> findByActivityIdAndStatus(Long activityId, ExamPaper.Status status);
    
    List<ExamPaper> findByStatus(ExamPaper.Status status);
    
    Optional<ExamPaper> findFirstByActivityIdAndStatus(Long activityId, ExamPaper.Status status);
}