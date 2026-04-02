package com.school.teacherEval.repository;

import com.school.teacherEval.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId ORDER BY e.createdAt DESC")
    List<Evaluation> findByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId AND e.periodId = :periodId")
    Optional<Evaluation> findByTeacherIdAndPeriodId(@Param("teacherId") Long teacherId, @Param("periodId") Long periodId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.periodId = :periodId ORDER BY e.createdAt DESC")
    Page<Evaluation> findByPeriodId(@Param("periodId") Long periodId, Pageable pageable);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId AND e.periodId = :periodId")
    Page<Evaluation> findByTeacherIdAndPeriodId(@Param("teacherId") Long teacherId, @Param("periodId") Long periodId, Pageable pageable);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId ORDER BY e.createdAt DESC")
    Page<Evaluation> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);
    
    boolean existsByTeacherIdAndPeriodId(Long teacherId, Long periodId);
    
    long countByPeriodId(Long periodId);
    
    long countByStatus(Evaluation.Status status);
}