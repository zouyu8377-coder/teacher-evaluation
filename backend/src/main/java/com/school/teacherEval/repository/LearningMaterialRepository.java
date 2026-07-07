package com.school.teacherEval.repository;

import com.school.teacherEval.entity.LearningMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
    
    @Query("SELECT m FROM LearningMaterial m WHERE m.isDeleted = 0 AND m.activityId = :activityId ORDER BY m.createdAt DESC")
    Page<LearningMaterial> findByActivityId(@Param("activityId") Long activityId, Pageable pageable);
    
    @Query("SELECT m FROM LearningMaterial m WHERE m.isDeleted = 0 ORDER BY m.createdAt DESC")
    Page<LearningMaterial> findAllActive(Pageable pageable);
    
    @Query("SELECT m FROM LearningMaterial m WHERE m.isDeleted = 0 AND m.id = :id")
    LearningMaterial findActiveById(@Param("id") Long id);

    List<LearningMaterial> findByActivityIdInAndIsDeleted(List<Long> activityIds, Integer isDeleted);
}
