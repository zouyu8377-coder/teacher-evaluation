package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;

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
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private DocumentService documentService;
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
        cActivity.setReviewerIds("[10, 11]");

        nonCActivity = new Activity();
        nonCActivity.setId(2L);
        nonCActivity.setLevel(Activity.Level.B1);
        nonCActivity.setReviewerIds("[10]");

        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setActivityId(2L);
        enrollment.setTeacherId(20L);
        enrollment.setStatus(PeriodEnrollment.Status.enrolled);
        enrollment.setMaterialStatus(PeriodEnrollment.MaterialStatus.submitted);
        lenient().when(enrollmentRepository.findByActivityIdAndTeacherId(2L, 20L)).thenReturn(Optional.of(enrollment));
        lenient().when(documentService.autoConfirmIfExpired(any(Activity.class), any(PeriodEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(documentService.isMaterialReviewable(any(Activity.class), any(PeriodEnrollment.class), eq(true)))
                .thenReturn(true);
    }

    @Test
    void validateUsersExist_shouldPass_whenBothExist() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(20L)).thenReturn(Optional.of(new User()));

        assertDoesNotThrow(() -> validator.validateUsersExist(10L, 20L));
    }

    @Test
    void validateUsersExist_shouldThrow_whenEvaluatorNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> validator.validateUsersExist(10L, 20L));
    }

    @Test
    void validateUsersExist_shouldThrow_whenTeacherNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> validator.validateUsersExist(10L, 20L));
    }

    @Test
    void validateEvaluatorPermission_shouldPass_whenAuthorized() {
        when(activityService.getById(2L)).thenReturn(nonCActivity);

        assertDoesNotThrow(() -> validator.validateEvaluatorPermission(10L, 2L));
    }

    @Test
    void validateEvaluatorPermission_shouldThrow_whenCLevel() {
        when(activityService.getById(1L)).thenReturn(cActivity);

        assertThrows(BusinessException.class, () -> validator.validateEvaluatorPermission(10L, 1L));
    }

    @Test
    void validateEvaluatorPermission_shouldThrow_whenReviewerIdsEmpty() {
        nonCActivity.setReviewerIds(null);
        when(activityService.getById(2L)).thenReturn(nonCActivity);

        assertThrows(BusinessException.class, () -> validator.validateEvaluatorPermission(10L, 2L));
    }

    @Test
    void validateEvaluatorPermission_shouldThrow_whenNotInList() {
        when(activityService.getById(2L)).thenReturn(nonCActivity);

        assertThrows(BusinessException.class, () -> validator.validateEvaluatorPermission(99L, 2L));
    }

    @Test
    void validateActivityOpenForEvaluation_shouldAlwaysPass_afterStatusRemoval() {
        assertDoesNotThrow(() -> validator.validateActivityOpenForEvaluation(1L));
    }

    @Test
    void validateScoreRange_shouldPass_forValidScore() {
        assertDoesNotThrow(() -> validator.validateScoreRange(new BigDecimal("85.5")));
    }

    @Test
    void validateScoreRange_shouldThrow_whenNull() {
        assertThrows(BusinessException.class, () -> validator.validateScoreRange(null));
    }

    @Test
    void validateScoreRange_shouldThrow_whenBelowZero() {
        assertThrows(BusinessException.class, () -> validator.validateScoreRange(new BigDecimal("-1")));
    }

    @Test
    void validateScoreRange_shouldThrow_whenAbove100() {
        assertThrows(BusinessException.class, () -> validator.validateScoreRange(new BigDecimal("100.1")));
    }

    @Test
    void validateScoreRange_shouldThrow_whenTwoDecimalPlaces() {
        assertThrows(BusinessException.class, () -> validator.validateScoreRange(new BigDecimal("85.55")));
    }

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

        assertThrows(BusinessException.class, () -> validator.validateTeacherCompletion(20L, 1L));
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

        assertThrows(BusinessException.class, () -> validator.validateTeacherCompletion(20L, 2L));
    }

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

        assertThrows(BusinessException.class, () -> validator.validateNotLocked(1L, 20L));
    }
}
