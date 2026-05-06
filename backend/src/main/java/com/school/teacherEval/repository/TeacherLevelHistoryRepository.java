package com.school.teacherEval.repository;

import com.school.teacherEval.entity.TeacherLevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherLevelHistoryRepository extends JpaRepository<TeacherLevelHistory, Long> {

    List<TeacherLevelHistory> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}
