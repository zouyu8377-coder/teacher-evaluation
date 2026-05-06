package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationValidatorTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityService activityService;
    @Mock
    private ExamRecordService examRecordService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private EvaluationValidator validator;

    private Activity cActivity;
    private Activity nonCActivity;

    @BeforeEach
    void setUp() {
        cActivity = new Activity();
        cActivity.setId(1L);
        cActivity.setLevel(Activity.Level.C);
        // 状态机制已移除，无需设置 status
        cActivity.setReviewerIds("[10, 11]");

        nonCActivity = new Activity();
        nonCActivity.setId(2L);
        nonCActivity.setLevel(Activity.Level.B1);
        // 状态机制已移除，无需设置 status
        nonCActivity.setReviewerIds("[10]");
    }

    // ================== validateUsersExist ==================

    @Test
    void validateUsersExist_shouldPass_whenBothExist() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(20L)).thenReturn(Optional.of(new User()));

        assertDoesNotThrow(() -> validator.validateUsersExist(10L, 20L));
    }

    @Test
    void validateUsersExist_shouldThrow_whenEvaluatorNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUsersExist(10L, 20L));
        assertEquals("考核员不存在", ex.getMessage());
    }

    @Test
    void validateUsersExist_shouldThrow_whenTeacherNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUsersExist(10L, 20L));
        assertEquals("教师不存在", ex.getMessage());
    }

    // ================== validateEvaluatorPermission ==================

    @Test
    void validateEvaluatorPermission_shouldPass_whenAuthorized() {
        when(activityService.getById(1L)).thenReturn(cActivity);

        assertDoesNotThrow(() -> validator.validateEvaluatorPermission(10L, 1L));
    }

    @Test
    void validateEvaluatorPermission_shouldThrow_whenReviewerIdsEmpty() {
        cActivity.setReviewerIds(null);
        when(activityService.getById(1L)).thenReturn(cActivity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateEvaluatorPermission(10L, 1L));
        assertEquals("该活动未分配考核员，无法评分", ex.getMessage());
    }

    @Test
    void validateEvaluatorPermission_shouldThrow_whenNotInList() {
        when(activityService.getById(1L)).thenReturn(cActivity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateEvaluatorPermission(99L, 1L));
        assertEquals("您无权对该活动进行评分", ex.getMessage());
    }

    // ================== validateActivityOpenForEvaluation ==================

    @Test
    void validateActivityOpenForEvaluation_shouldPass_whenActive() {
        when(activityService.getById(1L)).thenReturn(cActivity);

        assertDoesNotThrow(() -> validator.validateActivityOpenForEvaluation(1L));
    }

    @Test
    void validateActivityOpenForEvaluation_shouldThrow_whenClosed() {
        // 状态机制已移除，无需设置 status
        when(activityService.getById(1L)).thenReturn(cActivity);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateActivityOpenForEvaluation(1L));
        assertEquals("该活动已禁用，无法评分", ex.getMessage());
    }

    // ================== validateScoreRange ==================

    @Test
    void validateScoreRange_shouldPass_forValidScore() {
        assertDoesNotThrow(() -> validator.validateScoreRange(new BigDecimal("85.5")));
    }

    @Test
    void validateScoreRange_shouldThrow_whenNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateScoreRange(null));
        assertEquals("评分不能为空", ex.getMessage());
    }

    @Test
    void validateScoreRange_shouldThrow_whenBelowZero() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateScoreRange(new BigDecimal("-1")));
        assertEquals("评分必须在 0-100 之间", ex.getMessage());
    }

    @Test
    void validateScoreRange_shouldThrow_whenAbove100() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateScoreRange(new BigDecimal("100.1")));
        assertEquals("评分必须在 0-100 之间", ex.getMessage());
    }

    @Test
    void validateScoreRange_shouldThrow_whenTwoDecimalPlaces() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateScoreRange(new BigDecimal("85.55")));
        assertEquals("评分最多保留一位小数", ex.getMessage());
    }

    // ================== validateTeacherCompletion ==================

    @Test
    void validateTeacherCompletion_shouldPass_whenCLevelExamSubmitted() {
        when(activityService.getById(1L)).thenReturn(cActivity);
        ExamRecord record = new ExamRecord();
        record.setStatus(ExamRecord.Status.submitted);
        when(examRecordService.getRecordByTeacherAndActivity(20L, 1L)).thenReturn(record);

        assertDoesNotThrow(() -> validator.validateTeacherCompletion(20L, 1L));
    }

    @Test
    void validateTeacherCompletion_shouldThrow_whenCLevelExamNotSubmitted() {
        when(activityService.getById(1L)).thenReturn(cActivity);
        when(examRecordService.getRecordByTeacherAndActivity(20L, 1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateTeacherCompletion(20L, 1L));
        assertEquals("该教师尚未完成考试，无法评分", ex.getMessage());
    }

    @Test
    void validateTeacherCompletion_shouldPass_whenNonCLevelDocumentExists() {
        when(activityService.getById(2L)).thenReturn(nonCActivity);
        when(documentRepository.findFirstByActivityIdAndUserId(2L, 20L))
                .thenReturn(Optional.of(new Document()));

        assertDoesNotThrow(() -> validator.validateTeacherCompletion(20L, 2L));
    }

    @Test
    void validateTeacherCompletion_shouldThrow_whenNonCLevelDocumentMissing() {
        when(activityService.getById(2L)).thenReturn(nonCActivity);
        when(documentRepository.findFirstByActivityIdAndUserId(2L, 20L))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateTeacherCompletion(20L, 2L));
        assertEquals("该教师尚未上传考核文档，无法评分", ex.getMessage());
    }

    // ================== validateNotLocked ==================

    @Test
    void validateNotLocked_shouldPass_whenNoLock() {
        when(evaluationRepository.findByActivityIdAndTeacherId(1L, 20L))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validateNotLocked(1L, 20L));
    }

    @Test
    void validateNotLocked_shouldThrow_whenLocked() {
        Evaluation lockedEval = new Evaluation();
        lockedEval.setIsLocked(true);
        when(evaluationRepository.findByActivityIdAndTeacherId(1L, 20L))
                .thenReturn(List.of(lockedEval));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateNotLocked(1L, 20L));
        assertEquals("该教师的成绩已发布锁定，无法修改评分", ex.getMessage());
    }
}
