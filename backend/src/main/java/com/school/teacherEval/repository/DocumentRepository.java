package com.school.teacherEval.repository;

import com.school.teacherEval.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    @Query("SELECT d FROM Document d WHERE d.isDeleted = 0 AND d.userId = :userId ORDER BY d.createdAt DESC")
    Page<Document> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.isDeleted = 0 AND d.activityId = :activityId ORDER BY d.createdAt DESC")
    Page<Document> findByActivityId(@Param("activityId") Long activityId, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.isDeleted = 0 AND d.userId = :userId AND d.activityId = :activityId ORDER BY d.createdAt DESC")
    Page<Document> findByUserIdAndActivityId(@Param("userId") Long userId, @Param("activityId") Long activityId, Pageable pageable);
    
    @Query("SELECT d FROM Document d WHERE d.isDeleted = 0 ORDER BY d.createdAt DESC")
    Page<Document> findAllActive(Pageable pageable);
    
    long countByUserId(Long userId);
}