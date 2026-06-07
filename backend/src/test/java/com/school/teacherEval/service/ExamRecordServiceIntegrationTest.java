package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.ExamPaper;
import com.school.teacherEval.entity.ExamQuestion;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PaperQuestion;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.ExamPaperRepository;
import com.school.teacherEval.repository.ExamQuestionRepository;
import com.school.teacherEval.repository.ExamRecordRepository;
import com.school.teacherEval.repository.PaperQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExamRecordServiceIntegrationTest {

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private ExamRecordRepository recordRepository;
    @Autowired
    private ExamPaperRepository paperRepository;
    @Autowired
    private ExamQuestionRepository questionRepository;
    @Autowired
    private PaperQuestionRepository paperQuestionRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @MockBean
    private ActivityService activityService;
    @MockBean
    private EnrollmentService enrollmentService;

    private Activity testActivity;
    private ExamPaper testPaper;
    private ExamQuestion question1;
    private ExamQuestion question2;

    @BeforeEach
    void setUp() {
        // 创建试卷
        testPaper = new ExamPaper();
        testPaper.setName("C级测试试卷");
        testPaper.setTotalScore(10);
        testPaper.setDurationMinutes(60);
        testPaper.setQuestionCount(2);
        testPaper.setStatus(ExamPaper.Status.active);
        testPaper.setCreatedBy(1L);
        testPaper = paperRepository.save(testPaper);

        // 创建单选题（5分）
        question1 = new ExamQuestion();
        question1.setQuestionText("第一题");
        question1.setQuestionType(ExamQuestion.QuestionType.single);
        question1.setOptions("[{\"label\":\"A\",\"text\":\"选项A\"},{\"label\":\"B\",\"text\":\"选项B\"}]");
        question1.setCorrectAnswer("A");
        question1.setScore(5);
        question1.setCreatedBy(1L);
        question1 = questionRepository.save(question1);

        // 创建多选题（5分）
        question2 = new ExamQuestion();
        question2.setQuestionText("第二题");
        question2.setQuestionType(ExamQuestion.QuestionType.multiple);
        question2.setOptions("[{\"label\":\"A\",\"text\":\"选项A\"},{\"label\":\"B\",\"text\":\"选项B\"},{\"label\":\"C\",\"text\":\"选项C\"}]");
        question2.setCorrectAnswer("AC");
        question2.setScore(5);
        question2.setCreatedBy(1L);
        question2 = questionRepository.save(question2);

        // 关联试卷与题目
        PaperQuestion pq1 = new PaperQuestion();
        pq1.setPaperId(testPaper.getId());
        pq1.setQuestionId(question1.getId());
        pq1.setQuestion(question1);
        pq1.setQuestionOrder(1);
        paperQuestionRepository.save(pq1);

        PaperQuestion pq2 = new PaperQuestion();
        pq2.setPaperId(testPaper.getId());
        pq2.setQuestionId(question2.getId());
        pq2.setQuestion(question2);
        pq2.setQuestionOrder(2);
        paperQuestionRepository.save(pq2);

        // 创建活动
        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setName("C级测试活动");
        testActivity.setLevel(Activity.Level.C);
        // 状态机制已移除，无需设置 status
        testActivity.setHasExam(true);
        testActivity.setExamPaperId(testPaper.getId());
        testActivity.setExamStart(LocalDateTime.now().minusHours(1));
        testActivity.setExamEnd(LocalDateTime.now().plusHours(1));

        // Mock 外部服务
        when(activityService.getById(testActivity.getId())).thenReturn(testActivity);
        when(enrollmentService.isEnrolledByActivity(any(), any())).thenReturn(true);
    }

    @Test
    void fullExamFlow_shouldStartSaveSubmitAndGrade() {
        Long teacherId = 100L;
        Long activityId = testActivity.getId();

        // ========== 1. 开始考试 ==========
        ExamRecord record = examRecordService.startExam(activityId, teacherId);

        assertNotNull(record.getId());
        assertEquals(ExamRecord.Status.in_progress, record.getStatus());
        assertEquals(testPaper.getId(), record.getPaperId());
        assertNotNull(record.getStartedAt());

        // ========== 2. 保存答案 ==========
        Map<String, String> answers = Map.of(
                "1", "A",      // 单选正确
                "2", "AC"      // 多选正确
        );
        record = examRecordService.saveAnswer(record.getId(), answers, teacherId);
        assertNotNull(record.getAnswers());

        // ========== 3. 提交考试 ==========
        record = examRecordService.submitExam(record.getId(), teacherId);

        assertEquals(ExamRecord.Status.submitted, record.getStatus());
        assertNotNull(record.getSubmittedAt());
        assertEquals(new BigDecimal("10"), record.getAutoScore());
        assertEquals(new BigDecimal("10"), record.getScore());
        assertEquals(2, record.getCorrectCount());
        assertEquals(0, record.getWrongCount());

        // ========== 4. 同步到 Evaluation 表 ==========
        var evals = evaluationRepository.findByTeacherIdAndActivityId(teacherId, activityId);
        assertFalse(evals.isEmpty());
        Evaluation eval = evals.get(0);
        assertEquals(record.getId(), eval.getExamRecordId());
        assertEquals(new BigDecimal("10"), eval.getAutoScore());
        assertEquals(new BigDecimal("10"), eval.getScore());
    }

    @Test
    void fullExamFlow_shouldGradePartiallyCorrect() {
        Long teacherId = 101L;
        Long activityId = testActivity.getId();

        // 开始考试
        ExamRecord record = examRecordService.startExam(activityId, teacherId);

        // 单选错误，多选正确（排序不影响）
        Map<String, String> answers = Map.of(
                "1", "B",
                "2", "CA"
        );
        record = examRecordService.saveAnswer(record.getId(), answers, teacherId);
        record = examRecordService.submitExam(record.getId(), teacherId);

        assertEquals(new BigDecimal("5"), record.getAutoScore());
        assertEquals(1, record.getCorrectCount());
        assertEquals(1, record.getWrongCount());
    }

    @Test
    void startExam_shouldReturnExisting_whenInProgress() {
        Long teacherId = 102L;
        Long activityId = testActivity.getId();

        ExamRecord first = examRecordService.startExam(activityId, teacherId);
        ExamRecord second = examRecordService.startExam(activityId, teacherId);

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void startExam_shouldThrow_whenNotEnrolled() {
        when(enrollmentService.isEnrolledByActivity(testActivity.getId(), 999L)).thenReturn(false);

        com.school.teacherEval.exception.BusinessException ex = assertThrows(
                com.school.teacherEval.exception.BusinessException.class,
                () -> examRecordService.startExam(testActivity.getId(), 999L));
        assertEquals("您尚未报名该活动，无法参加考试", ex.getMessage());
    }

    @Test
    void startExam_shouldThrow_whenAlreadySubmitted() {
        Long teacherId = 103L;
        Long activityId = testActivity.getId();

        ExamRecord record = examRecordService.startExam(activityId, teacherId);
        Map<String, String> answers = Map.of("1", "A", "2", "AC");
        examRecordService.saveAnswer(record.getId(), answers, teacherId);
        examRecordService.submitExam(record.getId(), teacherId);

        com.school.teacherEval.exception.BusinessException ex = assertThrows(
                com.school.teacherEval.exception.BusinessException.class,
                () -> examRecordService.startExam(activityId, teacherId));
        assertEquals("您已参加过该考试，不可重复考试", ex.getMessage());
    }

    @Test
    void saveAnswer_shouldThrow_whenNotEnrolled() {
        Long teacherId = 104L;
        ExamRecord record = examRecordService.startExam(testActivity.getId(), teacherId);

        when(enrollmentService.isEnrolledByActivity(testActivity.getId(), teacherId)).thenReturn(false);

        com.school.teacherEval.exception.BusinessException ex = assertThrows(
                com.school.teacherEval.exception.BusinessException.class,
                () -> examRecordService.saveAnswer(record.getId(), Map.of("1", "A"), teacherId));
        assertEquals("您已被移出该活动，无法继续作答", ex.getMessage());
    }

    @Test
    void getExamQuestions_shouldReturnQuestions_withoutCorrectAnswer_whenInProgress() {
        Long teacherId = 105L;
        ExamRecord record = examRecordService.startExam(testActivity.getId(), teacherId);

        Map<String, Object> result = examRecordService.getExamQuestions(record.getId(), teacherId);

        assertNotNull(result.get("questions"));
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> questions = (java.util.List<Map<String, Object>>) result.get("questions");
        assertEquals(2, questions.size());
        // 考试中不返回正确答案
        assertNull(questions.get(0).get("correctAnswer"));
    }

    @Test
    void getExamQuestions_shouldHideCorrectAnswer_untilPublished() {
        Long teacherId = 106L;
        ExamRecord record = examRecordService.startExam(testActivity.getId(), teacherId);
        examRecordService.saveAnswer(record.getId(), Map.of("1", "A", "2", "AC"), teacherId);
        examRecordService.submitExam(record.getId(), teacherId);

        Map<String, Object> result = examRecordService.getExamQuestions(record.getId(), teacherId);

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> questions = (java.util.List<Map<String, Object>>) result.get("questions");
        assertNull(questions.get(0).get("correctAnswer"));
        assertNull(questions.get(1).get("correctAnswer"));
    }
}
