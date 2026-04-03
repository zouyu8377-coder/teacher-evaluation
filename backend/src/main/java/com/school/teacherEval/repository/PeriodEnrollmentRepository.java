package com.school.teacherEval.repository;

import com.school.teacherEval.entity.PeriodEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodEnrollmentRepository extends JpaRepository<PeriodEnrollment, Long> {
    
    @Query("SELECT e FROM PeriodEnrollment e WHERE e.periodId = :periodId AND e.teacherId = :teacherId AND e.status = 'enrolled'")
    Optional<PeriodEnrollment> findActiveEnrollment(@Param("periodId") Long periodId, @Param("teacherId") Long teacherId);
    
    @Query("SELECT e FROM PeriodEnrollment e WHERE e.periodId = :periodId AND e.status = 'enrolled'")
    List<PeriodEnrollment> findActiveEnrollmentsByPeriodId(@Param("periodId") Long periodId);
    
    @Query("SELECT e FROM PeriodEnrollment e WHERE e.teacherId = :teacherId AND e.status = 'enrolled'")
    List<PeriodEnrollment> findActiveEnrollmentsByTeacherId(@Param("teacherId") Long teacherId);
    
    boolean existsByPeriodIdAndTeacherIdAndStatus(Long periodId, Long teacherId, PeriodEnrollment.Status status);
}