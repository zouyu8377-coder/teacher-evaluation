package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.*;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExamRecordService {
    
    private final ExamRecordRepository recordRepository;
    private final ExamPaperRepository paperRepository;
    private final ExamQuestionRepository questionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ActivityService activityService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ExamRecord startExam(Long activityId, Long teacherId) {
        // 获取活动信息
        Activity activity = activityService.getById(activityId);
        if (!Boolean.TRUE.equals(activity.getHasExam()) || activity.getExamPaperId() == null) {
            throw new RuntimeException("该活动没有关联试卷");
        }

        // 检查当前时间是否在考试时间段内
        LocalDateTime now = LocalDateTime.now();
        if (activity.getExamStart() == null || activity.getExamEnd() == null) {
            throw new RuntimeException("该活动考试时间未设置");
        }
        if (now.isBefore(activity.getExamStart())) {
            throw new BusinessException("考试尚未开始，开始时间：" + activity.getExamStart().toString().replace("T", " "));
        }
        if (now.isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试已结束，结束时间：" + activity.getExamEnd().toString().replace("T", " "));
        }

        // 检查是否已有已提交的考试记录
        // 每个活动每个用户只有一次考试机会，未通过也不得重考
        Optional<ExamRecord> existing = recordRepository.findByTeacherIdAndActivityIdAndStatus(
            teacherId, activityId, ExamRecord.Status.submitted);
        if (existing.isPresent()) {
            // 已提交过考试，不允许再次参加
            throw new RuntimeException("您已参加过该考试，无法再次参加");
        }

        // 检查是否有进行中的考试
        Optional<ExamRecord> inProgress = recordRepository.findByTeacherIdAndActivityIdAndStatus(
            teacherId, activityId, ExamRecord.Status.in_progress);
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        // 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setPaperId(activity.getExamPaperId());
        record.setActivityId(activityId);
        record.setTeacherId(teacherId);
        record.setStatus(ExamRecord.Status.in_progress);
        record.setStartedAt(LocalDateTime.now());

        return recordRepository.save(record);
    }
    
    public ExamRecord getRecordById(Long id) {
        return recordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("考试记录不存在"));
    }
    
    public ExamRecord getRecordByTeacherAndActivity(Long teacherId, Long activityId) {
        List<ExamRecord> records = recordRepository.findByTeacherIdAndActivityId(teacherId, activityId);
        return records.isEmpty() ? null : records.get(0);
    }
    
    public List<ExamRecord> getMyRecords(Long teacherId) {
        return recordRepository.findByTeacherIdAndActivityId(teacherId, null);
    }
    
    public Page<ExamRecord> getRecordsByActivity(Long activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return recordRepository.findByActivityId(activityId, pageable);
    }
    
    public Map<String, Object> getExamQuestions(Long recordId, Long currentUserId) {
        ExamRecord record = getRecordById(recordId);
        
        // 校验权限
        if (!record.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("无权限查看");
        }
        
        ExamPaper paper = paperRepository.findById(record.getPaperId())
            .orElseThrow(() -> new RuntimeException("试卷不存在"));
        
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderByQuestionOrder(paper.getId());
        
        // 解析已有答案
        Map<String, String> answers = new HashMap<>();
        if (record.getAnswers() != null) {
            try {
                answers = objectMapper.readValue(record.getAnswers(), Map.class);
                answers = (Map<String, String>) (Map) answers;
            } catch (Exception e) {
                answers = new HashMap<>();
            }
        }
        
        // 构建返回数据（脱敏：考试中不返回正确答案）
        List<Map<String, Object>> questions = new ArrayList<>();
        boolean showAnswer = record.getStatus() == ExamRecord.Status.submitted;
        
        for (PaperQuestion pq : pqs) {
            ExamQuestion q = pq.getQuestion();
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("order", pq.getQuestionOrder());
            qMap.put("id", q.getId());
            qMap.put("text", q.getQuestionText());
            qMap.put("type", q.getQuestionType().name());
            qMap.put("options", parseOptions(q.getOptions()));
            qMap.put("score", q.getScore());
            qMap.put("userAnswer", answers.get(String.valueOf(pq.getQuestionOrder())));
            
            // 考试结束后返回正确答案和解析
            if (showAnswer) {
                qMap.put("correctAnswer", q.getCorrectAnswer());
                qMap.put("explanation", q.getExplanation());
                qMap.put("isCorrect", isAnswerCorrect(
                    answers.get(String.valueOf(pq.getQuestionOrder())), 
                    q.getCorrectAnswer(),
                    q.getQuestionType()
                ));
            }
            
            questions.add(qMap);
        }
        
        // 获取活动的考试时长（优先使用活动的设置）
        Integer durationMinutes = paper.getDurationMinutes();
        if (record.getActivityId() != null) {
            try {
                Activity activity = activityService.getById(record.getActivityId());
                if (activity.getExamDurationMinutes() != null && activity.getExamDurationMinutes() > 0) {
                    durationMinutes = activity.getExamDurationMinutes();
                }
            } catch (Exception e) {
                // 忽略活动获取失败，使用试卷默认时长
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paper", Map.of(
            "id", paper.getId(),
            "name", paper.getName(),
            "durationMinutes", durationMinutes,
            "totalScore", paper.getTotalScore()
        ));
        result.put("record", Map.of(
            "id", record.getId(),
            "status", record.getStatus().name(),
            "startedAt", record.getStartedAt(),
            "score", record.getScore() != null ? record.getScore() : "-",
            "correctCount", record.getCorrectCount() != null ? record.getCorrectCount() : 0,
            "wrongCount", record.getWrongCount() != null ? record.getWrongCount() : 0
        ));
        result.put("questions", questions);
        
        return result;
    }
    
    @Transactional
    public ExamRecord saveAnswer(Long recordId, Map<String, String> answers, Long currentUserId) {
        ExamRecord record = getRecordById(recordId);
        
        if (!record.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("无权限操作");
        }
        
        if (record.getStatus() != ExamRecord.Status.in_progress) {
            throw new RuntimeException("考试已结束，无法作答");
        }
        
        // 合并答案
        Map<String, String> existingAnswers = new HashMap<>();
        if (record.getAnswers() != null) {
            try {
                Map<String, Object> temp = objectMapper.readValue(record.getAnswers(), Map.class);
                for (String key : temp.keySet()) {
                    existingAnswers.put(key, String.valueOf(temp.get(key)));
                }
            } catch (Exception e) {
                existingAnswers = new HashMap<>();
            }
        }
        existingAnswers.putAll(answers);
        
        try {
            record.setAnswers(objectMapper.writeValueAsString(existingAnswers));
        } catch (Exception e) {
            throw new RuntimeException("保存答案失败");
        }
        
        return recordRepository.save(record);
    }
    
    @Transactional
    public ExamRecord submitExam(Long recordId, Long currentUserId) {
        ExamRecord record = getRecordById(recordId);
        
        if (!record.getTeacherId().equals(currentUserId)) {
            throw new RuntimeException("无权限操作");
        }
        
        if (record.getStatus() != ExamRecord.Status.in_progress) {
            throw new RuntimeException("考试已提交");
        }
        
        // 自动判分
        record = autoGrade(record);
        record.setSubmittedAt(LocalDateTime.now());
        record.setStatus(ExamRecord.Status.submitted);
        
        record = recordRepository.save(record);
        
        // 同步成绩到Evaluation表
        syncToEvaluation(record);
        
        return record;
    }
    
    public ExamRecord autoGrade(ExamRecord record) {
        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderByQuestionOrder(record.getPaperId());
        
        Map<String, String> answers = new HashMap<>();
        if (record.getAnswers() != null) {
            try {
                answers = objectMapper.readValue(record.getAnswers(), 
                    new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                answers = new HashMap<>();
            }
        }
        
        BigDecimal autoScore = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;
        
        for (PaperQuestion pq : pqs) {
            ExamQuestion q = pq.getQuestion();
            String userAnswer = answers.get(String.valueOf(pq.getQuestionOrder()));
            
            boolean isCorrect = isAnswerCorrect(userAnswer, q.getCorrectAnswer(), q.getQuestionType());
            
            if (isCorrect) {
                autoScore = autoScore.add(BigDecimal.valueOf(q.getScore()));
                correctCount++;
            } else {
                wrongCount++;
            }
        }
        
        record.setAutoScore(autoScore);
        record.setScore(autoScore.add(record.getManualAdjust() != null ? record.getManualAdjust() : BigDecimal.ZERO));
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        
        return record;
    }
    
    private boolean isAnswerCorrect(String userAnswer, String correctAnswer, ExamQuestion.QuestionType type) {
        if (userAnswer == null || correctAnswer == null) return false;
        
        userAnswer = userAnswer.toUpperCase().trim();
        correctAnswer = correctAnswer.toUpperCase().trim();
        
        if (type == ExamQuestion.QuestionType.single) {
            return userAnswer.equals(correctAnswer);
        } else {
            // 多选题：排序后比较
            char[] userChars = userAnswer.toCharArray();
            char[] correctChars = correctAnswer.toCharArray();
            Arrays.sort(userChars);
            Arrays.sort(correctChars);
            return new String(userChars).equals(new String(correctChars));
        }
    }
    
    private void syncToEvaluation(ExamRecord record) {
        // 查找或创建Evaluation记录
        List<Evaluation> evals = evaluationRepository.findByTeacherIdAndActivityId(
            record.getTeacherId(), record.getActivityId());
        
        Evaluation evaluation;
        if (!evals.isEmpty()) {
            evaluation = evals.get(0);
        } else {
            evaluation = new Evaluation();
            evaluation.setTeacherId(record.getTeacherId());
            evaluation.setActivityId(record.getActivityId());
            evaluation.setEvaluatorId(1L); // 系统 evaluator
        }
        
        evaluation.setExamRecordId(record.getId());
        evaluation.setAutoScore(record.getAutoScore());
        evaluation.setManualAdjust(record.getManualAdjust());
        evaluation.setScore(record.getScore());
        
        evaluationRepository.save(evaluation);
    }
    
    @Transactional
    public ExamRecord adjustScore(Long recordId, BigDecimal adjust, Long evaluatorId) {
        ExamRecord record = getRecordById(recordId);
        
        record.setManualAdjust(adjust != null ? adjust : BigDecimal.ZERO);
        
        // 重新计算总分
        BigDecimal autoScore = record.getAutoScore() != null ? record.getAutoScore() : BigDecimal.ZERO;
        record.setScore(autoScore.add(record.getManualAdjust()));
        
        record = recordRepository.save(record);
        
        // 同步到Evaluation
        syncToEvaluation(record);
        
        return record;
    }
    
    public Map<String, Object> getExamDetail(Long recordId) {
        ExamRecord record = getRecordById(recordId);
        ExamPaper paper = paperRepository.findById(record.getPaperId())
            .orElseThrow(() -> new RuntimeException("试卷不存在"));

        // 检查成绩是否已发布，只有发布后才能查看正确答案
        List<Evaluation> evals = evaluationRepository.findByTeacherIdAndActivityId(
            record.getTeacherId(), record.getActivityId());
        boolean isPublished = evals.stream()
            .anyMatch(e -> Boolean.TRUE.equals(e.getIsPublished()));

        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderByQuestionOrder(paper.getId());

        Map<String, String> answers = new HashMap<>();
        if (record.getAnswers() != null) {
            try {
                answers = objectMapper.readValue(record.getAnswers(),
                    new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                answers = new HashMap<>();
            }
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            ExamQuestion q = pq.getQuestion();
            String userAnswer = answers.get(String.valueOf(pq.getQuestionOrder()));

            Map<String, Object> qMap = new HashMap<>();
            qMap.put("order", pq.getQuestionOrder());
            qMap.put("text", q.getQuestionText());
            qMap.put("type", q.getQuestionType().name());
            qMap.put("options", parseOptions(q.getOptions()));
            qMap.put("score", q.getScore());
            qMap.put("userAnswer", userAnswer);

            // 只有成绩发布后才返回正确答案和解析
            if (isPublished) {
                qMap.put("correctAnswer", q.getCorrectAnswer());
                qMap.put("explanation", q.getExplanation());
                qMap.put("isCorrect", isAnswerCorrect(userAnswer, q.getCorrectAnswer(), q.getQuestionType()));
            }

            questions.add(qMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", Map.of(
            "id", record.getId(),
            "score", record.getScore(),
            "autoScore", record.getAutoScore(),
            "manualAdjust", record.getManualAdjust(),
            "correctCount", record.getCorrectCount(),
            "wrongCount", record.getWrongCount(),
            "startedAt", record.getStartedAt(),
            "submittedAt", record.getSubmittedAt(),
            "isPublished", isPublished
        ));
        result.put("questions", questions);

        return result;
    }
    
    private List<Map<String, String>> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}