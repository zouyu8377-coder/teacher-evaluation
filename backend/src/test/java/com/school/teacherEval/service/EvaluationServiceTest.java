package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityService activityService;
    @Mock
    private EvaluationValidator evaluationValidator;

    @InjectMocks
    private EvaluationService evaluationService;

    private Activity activeActivity;
    private Activity closedActivity;

    @BeforeEach
    void setUp() {
        activeActivity = new Activity();
        activeActivity.setId(1L);
        // 状态机制已移除，无需设置 status
        activeActivity.setReviewerIds("[10, 11]");
        activeActivity.setExamEnd(LocalDateTime.now().minusHours(1));

        closedActivity = new Activity();
        closedActivity.setId(1L);
        // 状态机制已移除，无需设置 status
        closedActivity.setReviewerIds("[10]");
        closedActivity.setExamEnd(LocalDateTime.now().minusHours(1));
    }

    // ================== getEvaluationById ==================

    @Test
    void getEvaluationById_shouldReturn_whenExists() {
        Evaluation eval = new Evaluation();
        eval.setId(1L);
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(eval));

        Evaluation result = evaluationService.getEvaluationById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEvaluationById_shouldThrow_whenNotExists() {
        when(evaluationRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.getEvaluationById(1L));
        assertEquals("评分记录不存在", ex.getMessage());
    }

    // ================== createOrUpdateEvaluation ==================

    @Test
    void createOrUpdateEvaluation_shouldCreateNew_whenNotExists() {
        doNothing().when(evaluationValidator).validateForSubmit(10L, 20L, 1L, new BigDecimal("85"));
        when(evaluationRepository.findByTeacherIdAndActivityIdAndEvaluatorId(20L, 1L, 10L))
                .thenReturn(Optional.empty());

        Evaluation saved = new Evaluation();
        saved.setId(1L);
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(saved);

        Evaluation result = evaluationService.createOrUpdateEvaluation(
                10L, 20L, 1L, new BigDecimal("85"), "good");

        assertNotNull(result);
        verify(evaluationValidator).validateForSubmit(10L, 20L, 1L, new BigDecimal("85"));
        verify(evaluationRepository).save(argThat(e ->
                e.getActivityId().equals(1L) &&
                e.getEvaluatorId().equals(10L) &&
                e.getTeacherId().equals(20L) &&
                e.getScore().equals(new BigDecimal("85")) &&
                e.getComment().equals("good") &&
                e.getStatus() == Evaluation.Status.submitted
        ));
    }

    @Test
    void createOrUpdateEvaluation_shouldUpdate_whenExists() {
        doNothing().when(evaluationValidator).validateForSubmit(10L, 20L, 1L, new BigDecimal("90"));

        Evaluation existing = new Evaluation();
        existing.setId(5L);
        existing.setScore(new BigDecimal("80"));
        when(evaluationRepository.findByTeacherIdAndActivityIdAndEvaluatorId(20L, 1L, 10L))
                .thenReturn(Optional.of(existing));

        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));

        Evaluation result = evaluationService.createOrUpdateEvaluation(
                10L, 20L, 1L, new BigDecimal("90"), "updated");

        assertEquals(5L, result.getId());
        assertEquals(new BigDecimal("90"), result.getScore());
        assertEquals("updated", result.getComment());
    }

    // ================== publishScores ==================

    @Test
    void publishScores_shouldThrow_whenExamNotEnded() {
        activeActivity.setExamEnd(LocalDateTime.now().plusHours(1));
        when(activityService.getById(1L)).thenReturn(activeActivity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.publishScores(1L, null, new BigDecimal("60")));
        assertEquals("考试时间尚未结束，无法公布成绩", ex.getMessage());
    }

    @Test
    void publishScores_shouldThrow_whenAlreadyPublished() {
        activeActivity.setScoresPublished(true);
        when(activityService.getById(1L)).thenReturn(activeActivity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.publishScores(1L, null, new BigDecimal("60")));
        assertEquals("该考核活动的成绩已公布，无法重复公布", ex.getMessage());
    }

    @Test
    void publishScores_shouldPublish_andLockScores() {
        when(activityService.getById(1L)).thenReturn(activeActivity);

        Evaluation eval1 = new Evaluation();
        eval1.setEvaluatorId(10L);
        eval1.setTeacherId(100L);
        eval1.setScore(new BigDecimal("80"));
        eval1.setStatus(Evaluation.Status.submitted);

        Evaluation eval2 = new Evaluation();
        eval2.setEvaluatorId(11L);
        eval2.setTeacherId(100L);
        eval2.setScore(new BigDecimal("90"));
        eval2.setStatus(Evaluation.Status.submitted);

        when(evaluationRepository.findByActivityId(1L))
                .thenReturn(List.of(eval1, eval2));

        int count = evaluationService.publishScores(1L, null, new BigDecimal("60"));

        assertEquals(2, count);
        assertTrue(eval1.getIsPublished());
        assertTrue(eval1.getIsLocked());
        assertEquals(new BigDecimal("85.00"), eval1.getFinalScore());
        assertTrue(activeActivity.getScoresPublished());
        verify(activityRepository).save(activeActivity);
    }

    @Test
    void publishScores_shouldExcludeSystemEvaluator_whenCalculatingAverage() {
        when(activityService.getById(1L)).thenReturn(activeActivity);

        Evaluation sysEval = new Evaluation();
        sysEval.setEvaluatorId(1L); // SYSTEM_EVALUATOR_ID
        sysEval.setTeacherId(100L);
        sysEval.setScore(new BigDecimal("60"));
        sysEval.setStatus(Evaluation.Status.submitted);

        Evaluation humanEval = new Evaluation();
        humanEval.setEvaluatorId(10L);
        humanEval.setTeacherId(100L);
        humanEval.setScore(new BigDecimal("90"));
        humanEval.setStatus(Evaluation.Status.submitted);

        when(evaluationRepository.findByActivityId(1L))
                .thenReturn(List.of(sysEval, humanEval));

        int count = evaluationService.publishScores(1L, null, new BigDecimal("60"));

        assertEquals(1, count);
        assertEquals(new BigDecimal("90.00"), humanEval.getFinalScore());
        // 系统评分不应被修改
        assertNull(sysEval.getFinalScore());
        assertFalse(sysEval.getIsPublished());
    }

    // ================== calculateAverageScore ==================

    @Test
    void calculateAverageScore_shouldReturnZero_forEmptyList() {
        assertEquals(BigDecimal.ZERO, evaluationService.calculateAverageScore(List.of()));
        assertEquals(BigDecimal.ZERO, evaluationService.calculateAverageScore(null));
    }

    @Test
    void calculateAverageScore_shouldCalculateCorrectly() {
        Evaluation e1 = new Evaluation();
        e1.setScore(new BigDecimal("80"));
        Evaluation e2 = new Evaluation();
        e2.setScore(new BigDecimal("90"));

        BigDecimal avg = evaluationService.calculateAverageScore(List.of(e1, e2));
        assertEquals(new BigDecimal("85.00"), avg);
    }

    @Test
    void calculateAverageScore_shouldIgnoreNullScores() {
        Evaluation e1 = new Evaluation();
        e1.setScore(new BigDecimal("80"));
        Evaluation e2 = new Evaluation();
        e2.setScore(null);

        BigDecimal avg = evaluationService.calculateAverageScore(List.of(e1, e2));
        assertEquals(new BigDecimal("80.00"), avg);
    }

    // ================== getEvaluations ==================

    @Test
    void getEvaluations_shouldPaginate_whenBothIdsProvided() {
        Evaluation e1 = new Evaluation();
        Evaluation e2 = new Evaluation();
        when(evaluationRepository.findByActivityIdAndTeacherId(1L, 10L))
                .thenReturn(List.of(e1, e2));

        Page<Evaluation> page = evaluationService.getEvaluations(1L, 10L, 1, 10);
        assertEquals(2, page.getTotalElements());
    }
}
