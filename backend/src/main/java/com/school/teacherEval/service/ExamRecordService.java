package com.school.teacherEval.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teacherEval.entity.*;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamRecordService {
    
    @PersistenceContext
    private EntityManager entityManager;

    private final ExamRecordRepository recordRepository;
    private final ExamPaperRepository paperRepository;
    private final ExamQuestionRepository questionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final EvaluationRepository evaluationRepository;
    private final ActivityService activityService;
    private final EnrollmentService enrollmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ExamRecord startExam(Long activityId, Long teacherId) {
        // 获取活动信息
        Activity activity = activityService.getById(activityId);
        if (!Boolean.TRUE.equals(activity.getHasExam()) || activity.getExamPaperId() == null) {
            throw new BusinessException("该活动没有关联试卷");
        }

        // 校验教师是否已报名
        if (!enrollmentService.isEnrolledByActivity(activityId, teacherId)) {
            throw new BusinessException("您尚未报名该活动，无法参加考试");
        }

        // 检查当前时间是否在考试时间段内
        LocalDateTime now = LocalDateTime.now();
        if (activity.getExamStart() == null || activity.getExamEnd() == null) {
            throw new BusinessException("该活动考试时间未设置");
        }
        if (now.isBefore(activity.getExamStart())) {
            throw new BusinessException("考试尚未开始，开始时间：" + activity.getExamStart().toString().replace("T", " "));
        }
        if (now.isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试已结束，结束时间：" + activity.getExamEnd().toString().replace("T", " "));
        }

        // 检查是否已有考试记录，每个活动每个用户只有一次考试机会
        List<ExamRecord> existingRecords = recordRepository.findByTeacherIdAndActivityId(teacherId, activityId);

        Optional<ExamRecord> inProgress = existingRecords.stream()
            .filter(r -> r.getStatus() == ExamRecord.Status.in_progress)
            .findFirst();

        if (inProgress.isPresent()) {
            // 有进行中的考试，直接返回继续作答
            return inProgress.get();
        }

        if (!existingRecords.isEmpty()) {
            // 已参加过该考试（不论是否通过），不允许再次参加
            throw new BusinessException("您已参加过该考试，不可重复考试");
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
        try {
            return entityManager.createQuery(
                    "SELECT r FROM ExamRecord r WHERE r.teacherId = :teacherId AND r.activityId = :activityId ORDER BY r.id DESC",
                    ExamRecord.class)
                .setParameter("teacherId", teacherId)
                .setParameter("activityId", activityId)
                .setMaxResults(1)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public List<ExamRecord> getMyRecords(Long teacherId) {
        return recordRepository.findByTeacherId(teacherId);
    }
    
    public Page<ExamRecord> getRecordsByActivity(Long activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return recordRepository.findByActivityId(activityId, pageable);
    }
    
    public Map<String, Object> getExamQuestions(Long recordId, Long currentUserId) {
        ExamRecord record = getRecordById(recordId);

        // 校验权限
        if (!record.getTeacherId().equals(currentUserId)) {
            throw new BusinessException("无权限查看");
        }

        // 校验当前时间是否在考试窗口内（防止考试时间结束后仍继续作答）
        // 已提交的考试记录允许查看，不受考试结束时间限制
        Activity activity = activityService.getById(record.getActivityId());
        LocalDateTime now = LocalDateTime.now();
        if (record.getStatus() != ExamRecord.Status.submitted
                && activity.getExamEnd() != null && now.isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试时间已结束，无法查看题目");
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
        boolean showAnswer = isScorePublished(record);
        
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
                Activity act = activityService.getById(record.getActivityId());
                if (act.getExamDurationMinutes() != null && act.getExamDurationMinutes() > 0) {
                    durationMinutes = act.getExamDurationMinutes();
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
            "activityId", record.getActivityId(),
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
            throw new BusinessException("无权限操作");
        }

        if (record.getStatus() != ExamRecord.Status.in_progress) {
            throw new BusinessException("考试已结束，无法作答");
        }

        // 校验教师是否仍具有报名资格（防止被踢出后仍继续考试）
        if (!enrollmentService.isEnrolledByActivity(record.getActivityId(), currentUserId)) {
            throw new BusinessException("您已被移出该活动，无法继续作答");
        }

        // 校验考试时间是否已结束
        Activity activity = activityService.getById(record.getActivityId());
        LocalDateTime now = LocalDateTime.now();
        if (activity.getExamEnd() != null && now.isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试时间已结束，无法保存答案");
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
            throw new BusinessException("保存答案失败");
        }
        
        return recordRepository.save(record);
    }
    
    @Transactional
    public ExamRecord submitExam(Long recordId, Long currentUserId) {
        ExamRecord record = getRecordById(recordId);

        if (!record.getTeacherId().equals(currentUserId)) {
            throw new BusinessException("无权限操作");
        }

        if (record.getStatus() != ExamRecord.Status.in_progress) {
            throw new BusinessException("考试已提交");
        }

        // 校验教师是否仍具有报名资格（防止被踢出后仍提交）
        if (!enrollmentService.isEnrolledByActivity(record.getActivityId(), currentUserId)) {
            throw new BusinessException("您已被移出该活动，无法提交考试");
        }

        // 校验考试时间是否已结束
        Activity activity = activityService.getById(record.getActivityId());
        LocalDateTime now = LocalDateTime.now();
        if (activity.getExamEnd() != null && now.isAfter(activity.getExamEnd())) {
            throw new BusinessException("考试时间已结束，无法提交");
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

        log.info("Auto-grading recordId={}, paperId={}, answerCount={}, totalQuestions={}",
            record.getId(), record.getPaperId(), answers.size(), pqs.size());

        for (PaperQuestion pq : pqs) {
            ExamQuestion q = pq.getQuestion();
            String userAnswer = answers.get(String.valueOf(pq.getQuestionOrder()));

            boolean isCorrect = isAnswerCorrect(userAnswer, q.getCorrectAnswer(), q.getQuestionType());

            log.info("Grading question order={}, userAnswer='{}', correctAnswer='{}', type={}, isCorrect={}",
                pq.getQuestionOrder(), userAnswer, q.getCorrectAnswer(), q.getQuestionType(), isCorrect);

            if (isCorrect) {
                autoScore = autoScore.add(BigDecimal.valueOf(q.getScore()));
                correctCount++;
            } else {
                wrongCount++;
            }
        }

        log.info("Auto-grade complete recordId={}, correctCount={}, wrongCount={}, autoScore={}",
            record.getId(), correctCount, wrongCount, autoScore);

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
    
    private static final Long SYSTEM_EVALUATOR_ID = 1L;

    private void syncToEvaluation(ExamRecord record) {
        // 精确查找系统Evaluation记录，避免覆盖考核员评分
        Optional<Evaluation> sysEvalOpt = evaluationRepository.findByTeacherIdAndActivityIdAndEvaluatorId(
            record.getTeacherId(), record.getActivityId(), SYSTEM_EVALUATOR_ID);

        Evaluation evaluation;
        if (sysEvalOpt.isPresent()) {
            evaluation = sysEvalOpt.get();
        } else {
            evaluation = new Evaluation();
            evaluation.setTeacherId(record.getTeacherId());
            evaluation.setActivityId(record.getActivityId());
            evaluation.setEvaluatorId(SYSTEM_EVALUATOR_ID);
        }

        evaluation.setExamRecordId(record.getId());
        evaluation.setAutoScore(record.getAutoScore());
        evaluation.setManualAdjust(record.getManualAdjust());
        evaluation.setScore(record.getScore());
        evaluation.setStatus(Evaluation.Status.submitted);

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
    
    public Map<String, Object> getExamDetail(Long recordId, Long viewerId, boolean isEvaluatorOrAdmin) {
        ExamRecord record = getRecordById(recordId);
        ExamPaper paper = paperRepository.findById(record.getPaperId())
            .orElseThrow(() -> new RuntimeException("试卷不存在"));

        // 检查成绩是否已发布
        boolean isPublished = isScorePublished(record);

        boolean showDetail = isPublished || isEvaluatorOrAdmin;

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

            // 成绩已发布，或考生查看自己的已提交记录，返回正确答案和解析
            if (showDetail) {
                qMap.put("correctAnswer", q.getCorrectAnswer());
                qMap.put("explanation", q.getExplanation());
                qMap.put("isCorrect", isAnswerCorrect(userAnswer, q.getCorrectAnswer(), q.getQuestionType()));
            }

            questions.add(qMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", Map.of(
            "id", record.getId(),
            "activityId", record.getActivityId(),
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
    
    private boolean isScorePublished(ExamRecord record) {
        List<Evaluation> evals = evaluationRepository.findByTeacherIdAndActivityId(
            record.getTeacherId(), record.getActivityId());
        return evals.stream().anyMatch(e -> Boolean.TRUE.equals(e.getIsPublished()));
    }

    private List<Map<String, String>> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
