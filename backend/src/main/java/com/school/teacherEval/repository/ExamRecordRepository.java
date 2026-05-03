package com.school.teacherEval.repository;

import com.school.teacherEval.entity.ExamRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {

    List<ExamRecord> findByStatus(ExamRecord.Status status);

    List<ExamRecord> findByTeacherIdAndActivityId(Long teacherId, Long activityId);

    List<ExamRecord> findByTeacherId(Long teacherId);

    Optional<ExamRecord> findFirstByTeacherIdAndActivityIdOrderByIdDesc(Long teacherId, Long activityId);

    Optional<ExamRecord> findByTeacherIdAndActivityIdAndStatus(Long teacherId, Long activityId, ExamRecord.Status status);
    
    Page<ExamRecord> findByActivityId(Long activityId, Pageable pageable);
    
    Page<ExamRecord> findByActivityIdAndStatus(Long activityId, ExamRecord.Status status, Pageable pageable);
    
    boolean existsByTeacherIdAndActivityIdAndStatus(Long teacherId, Long activityId, ExamRecord.Status status);
    
    long countByActivityIdAndStatus(Long activityId, ExamRecord.Status status);
}