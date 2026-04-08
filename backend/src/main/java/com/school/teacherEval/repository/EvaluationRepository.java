package com.school.teacherEval.repository;

import com.school.teacherEval.entity.Activity;
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
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId AND e.activityId = :activityId")
    List<Evaluation> findByTeacherIdAndActivityId(@Param("teacherId") Long teacherId, @Param("activityId") Long activityId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId AND e.activityId = :activityId AND e.finalScore >= :minScore")
    Optional<Evaluation> findByTeacherIdAndActivityIdAndScoreGreaterThanEqual(@Param("teacherId") Long teacherId, @Param("activityId") Long activityId, @Param("minScore") int minScore);
    
    @Query("SELECT e FROM Evaluation e WHERE e.activityId = :activityId ORDER BY e.createdAt DESC")
    Page<Evaluation> findByActivityId(@Param("activityId") Long activityId, Pageable pageable);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId ORDER BY e.createdAt DESC")
    Page<Evaluation> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);
    
    Optional<Evaluation> findByTeacherIdAndActivityIdAndEvaluatorId(@Param("teacherId") Long teacherId, @Param("activityId") Long activityId, @Param("evaluatorId") Long evaluatorId);

    @Query("SELECT e FROM Evaluation e WHERE e.examRecordId = :examRecordId")
    Optional<Evaluation> findByExamRecordId(@Param("examRecordId") Long examRecordId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.activityId = :activityId")
    List<Evaluation> findByActivityId(@Param("activityId") Long activityId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.teacherId = :teacherId AND e.isPublished = true ORDER BY e.createdAt DESC")
    List<Evaluation> findByTeacherIdAndIsPublished(@Param("teacherId") Long teacherId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.evaluatorId = :evaluatorId ORDER BY e.createdAt DESC")
    List<Evaluation> findByEvaluatorId(@Param("evaluatorId") Long evaluatorId);
    
    @Query("SELECT e FROM Evaluation e WHERE e.activityId = :activityId AND e.teacherId = :teacherId")
    List<Evaluation> findByActivityIdAndTeacherId(@Param("activityId") Long activityId, @Param("teacherId") Long teacherId);
    
    boolean existsByTeacherIdAndActivityId(Long teacherId, Long activityId);
    
    long countByActivityId(Long activityId);
    
    long countByStatus(Evaluation.Status status);
    
    @Query("SELECT AVG(e.score) FROM Evaluation e WHERE e.activityId = :activityId AND e.teacherId = :teacherId")
    Double calculateAverageScore(@Param("activityId") Long activityId, @Param("teacherId") Long teacherId);
    
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.activityId = :activityId AND e.teacherId = :teacherId AND e.score IS NOT NULL")
    long countCompletedEvaluations(@Param("activityId") Long activityId, @Param("teacherId") Long teacherId);
    
    @Query("SELECT COUNT(DISTINCT e.teacherId) FROM Evaluation e JOIN Activity a ON e.activityId = a.id WHERE a.level = :level AND e.finalScore >= 60 AND e.isPublished = true")
    long countPassedTeachersByLevel(@Param("level") Activity.Level level);
}