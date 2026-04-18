package com.school.teacherEval.repository;

import com.school.teacherEval.entity.ExamQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    Page<ExamQuestion> findByQuestionType(ExamQuestion.QuestionType type, Pageable pageable);

    @Query("SELECT q FROM ExamQuestion q WHERE " +
           "q.questionType = :type ORDER BY RAND()")
    List<ExamQuestion> findRandomQuestions(@Param("type") ExamQuestion.QuestionType type,
                                            Pageable pageable);

    @Query("SELECT q FROM ExamQuestion q ORDER BY RAND()")
    List<ExamQuestion> findRandomQuestions(Pageable pageable);
}