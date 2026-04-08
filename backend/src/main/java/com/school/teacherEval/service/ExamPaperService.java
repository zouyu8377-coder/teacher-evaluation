package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.ExamPaper;
import com.school.teacherEval.entity.ExamQuestion;
import com.school.teacherEval.entity.PaperQuestion;
import com.school.teacherEval.repository.ExamPaperRepository;
import com.school.teacherEval.repository.ExamQuestionRepository;
import com.school.teacherEval.repository.PaperQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamPaperService {
    
    private final ExamPaperRepository paperRepository;
    private final ExamQuestionRepository questionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final ActivityService activityService;
    
    public Page<ExamPaper> getPapers(Long activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        return paperRepository.findByActivityId(activityId, pageable);
    }
    
    public List<ExamPaper> getPapersByActivity(Long activityId) {
        return paperRepository.findByActivityId(activityId);
    }
    
    public ExamPaper getById(Long id) {
        return paperRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("试卷不存在"));
    }
    
    @Transactional
    public ExamPaper create(ExamPaper paper) {
        paper.setId(null);
        paper.setStatus(ExamPaper.Status.draft);
        return paperRepository.save(paper);
    }
    
    @Transactional
    public ExamPaper update(Long id, ExamPaper updated) {
        ExamPaper paper = getById(id);
        
        if (updated.getQuestionCount() != null && !updated.getQuestionCount().equals(paper.getQuestionCount())) {
            paper.setQuestionCount(updated.getQuestionCount());
        }
        
        paper.setName(updated.getName());
        paper.setDescription(updated.getDescription());
        paper.setTotalScore(updated.getTotalScore());
        paper.setDurationMinutes(updated.getDurationMinutes());
        paper.setStatus(updated.getStatus());
        
        return paperRepository.save(paper);
    }
    
    @Transactional
    public void delete(Long id) {
        paperRepository.deleteById(id);
    }
    
    public List<PaperQuestion> getPaperQuestions(Long paperId) {
        return paperQuestionRepository.findByPaperIdWithQuestions(paperId);
    }
    
    @Transactional
    public void setPaperQuestions(Long paperId, List<Long> questionIds) {
        paperQuestionRepository.deleteByPaperId(paperId);
        
        ExamPaper paper = getById(paperId);
        List<PaperQuestion> pqs = new ArrayList<>();
        
        for (int i = 0; i < questionIds.size(); i++) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(questionIds.get(i));
            pq.setQuestionOrder(i + 1);
            pqs.add(pq);
        }
        
        paperQuestionRepository.saveAll(pqs);
        
        paper.setQuestionCount(questionIds.size());
        paperRepository.save(paper);
    }
    
    @Transactional
    public ExamPaper generateRandomPaper(Long paperId, Integer singleCount, Integer multiCount) {
        ExamPaper paper = getById(paperId);
        
        List<ExamQuestion> singleQuestions = questionRepository.findRandomQuestions(
            ExamQuestion.QuestionType.single,
            PageRequest.of(0, singleCount)
        );
        
        List<ExamQuestion> multiQuestions = questionRepository.findRandomQuestions(
            ExamQuestion.QuestionType.multiple,
            PageRequest.of(0, multiCount)
        );
        
        List<Long> questionIds = new ArrayList<>();
        questionIds.addAll(singleQuestions.stream().map(ExamQuestion::getId).toList());
        questionIds.addAll(multiQuestions.stream().map(ExamQuestion::getId).toList());
        
        setPaperQuestions(paperId, questionIds);
        
        int totalScore = 0;
        for (Long qid : questionIds) {
            ExamQuestion q = questionRepository.findById(qid).orElse(null);
            if (q != null) {
                totalScore += q.getScore();
            }
        }
        paper.setTotalScore(totalScore);
        
        return paperRepository.save(paper);
    }
    
    @Transactional
    public void bindToActivity(Long paperId, Long activityId) {
        Activity activity = activityService.getById(activityId);
        
        if (activity.getLevel() != Activity.Level.C) {
            throw new RuntimeException("只有C级活动才能绑定试卷");
        }
        
        ExamPaper paper = getById(paperId);
        
        paper.setActivityId(activityId);
        paperRepository.save(paper);
        
        activity.setExamPaperId(paperId);
        activity.setHasExam(true);
        if (paper.getDurationMinutes() != null) {
            activity.setExamDurationMinutes(paper.getDurationMinutes());
        }
        activityService.update(activityId, activity);
    }
    
    public int getQuestionCount(Long paperId) {
        return (int) paperQuestionRepository.countByPaperId(paperId);
    }
}