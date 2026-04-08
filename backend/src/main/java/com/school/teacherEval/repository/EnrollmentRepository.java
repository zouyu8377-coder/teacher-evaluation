package com.school.teacherEval.repository;

import com.school.teacherEval.entity.PeriodEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<PeriodEnrollment, Long> {
    
    List<PeriodEnrollment> findByTeacherId(Long teacherId);
    
    List<PeriodEnrollment> findByActivityId(Long activityId);
    
    Optional<PeriodEnrollment> findByActivityIdAndTeacherId(Long activityId, Long teacherId);
    
    boolean existsByActivityIdAndTeacherIdAndStatus(Long activityId, Long teacherId, PeriodEnrollment.Status status);
    
    long countByActivityIdAndStatus(Long activityId, PeriodEnrollment.Status status);
}