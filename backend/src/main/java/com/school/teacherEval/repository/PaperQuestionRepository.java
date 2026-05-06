package com.school.teacherEval.repository;

import com.school.teacherEval.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    
    List<PaperQuestion> findByPaperIdOrderByQuestionOrder(Long paperId);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByPaperId(Long paperId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM PaperQuestion pq WHERE pq.paperId = :paperId")
    int deleteByPaperIdAndFlush(@Param("paperId") Long paperId);
    
    @Query("SELECT pq FROM PaperQuestion pq JOIN FETCH pq.question WHERE pq.paperId = :paperId ORDER BY pq.questionOrder")
    List<PaperQuestion> findByPaperIdWithQuestions(@Param("paperId") Long paperId);
    
    long countByPaperId(Long paperId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByQuestionId(Long questionId);
}