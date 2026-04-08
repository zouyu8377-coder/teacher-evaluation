package com.school.teacherEval.repository;

import com.school.teacherEval.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    List<Activity> findByStatus(Activity.Status status);
    
    List<Activity> findByLevel(Activity.Level level);
    
    List<Activity> findByLevelAndStatus(Activity.Level level, Activity.Status status);
    
    @Query("SELECT a FROM Activity a WHERE a.level = :level AND a.status = 'active'")
    Optional<Activity> findActiveByLevel(@Param("level") Activity.Level level);
    
    @Query("SELECT a FROM Activity a WHERE a.status = 'active' AND a.enrollmentStart <= CURRENT_TIMESTAMP AND a.enrollmentEnd >= CURRENT_TIMESTAMP")
    List<Activity> findAvailableActivities();
    
    @Query("SELECT a FROM Activity a WHERE a.status = 'active' AND a.startDate <= :date AND a.endDate >= :date")
    List<Activity> findActiveByDate(@Param("date") LocalDate date);
    
    @Query("SELECT a FROM Activity a WHERE a.status = 'active' ORDER BY CASE a.level WHEN 'C' THEN 1 WHEN 'B2' THEN 2 WHEN 'B1' THEN 3 WHEN 'A2' THEN 4 WHEN 'A1' THEN 5 END, a.startDate DESC")
    List<Activity> findAllActiveOrderByLevel();
    
    @Query("SELECT a FROM Activity a WHERE a.reviewerIds LIKE %:evaluatorId%")
    List<Activity> findByReviewerId(@Param("evaluatorId") Long evaluatorId);
}