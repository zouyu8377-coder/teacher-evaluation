package com.school.teacherEval.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "paper_questions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"paper_id", "question_order"})
})
public class PaperQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "paper_id", nullable = false)
    private Long paperId;
    
    @Column(name = "question_id", nullable = false)
    private Long questionId;
    
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private ExamQuestion question;
}