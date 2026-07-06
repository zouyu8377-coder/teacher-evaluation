package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.ExamRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private ExamRecordRepository examRecordRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ActivityValidator activityValidator;
    @Mock
    private EnrollmentEligibilityService enrollmentEligibilityService;

    @InjectMocks
    private ActivityService activityService;

    private Activity activity;

    @BeforeEach
    void setUp() {
        activity = new Activity();
        activity.setId(1L);
        activity.setName("Activity");
        activity.setLevel(Activity.Level.C);
        activity.setEnrollmentStart(LocalDateTime.now().minusDays(2));
        activity.setEnrollmentEnd(LocalDateTime.now().minusDays(1));
        activity.setExamStart(LocalDateTime.now().plusDays(1));
        activity.setExamEnd(LocalDateTime.now().plusDays(1).plusHours(1));
        activity.setExamDurationMinutes(60);
        activity.setExamPaperId(10L);
        activity.setReviewerIds("[10]");
    }

    @Test
    void update_shouldRejectCriticalTimeChange_whenActivityHasEnrollment() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.countByActivityIdAndStatus(1L, PeriodEnrollment.Status.enrolled)).thenReturn(1L);

        Activity updated = new Activity();
        updated.setExamStart(activity.getExamStart().plusHours(1));

        assertThrows(BusinessException.class, () -> activityService.update(1L, updated));
    }

    @Test
    void update_shouldAllowDescriptionChange_whenActivityHasEnrollment() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(enrollmentRepository.countByActivityIdAndStatus(1L, PeriodEnrollment.Status.enrolled)).thenReturn(1L);
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activity updated = new Activity();
        updated.setDescription("New description");

        Activity result = activityService.update(1L, updated);

        assertEquals("New description", result.getDescription());
        verify(activityRepository).save(activity);
    }

    @Test
    void create_shouldNormalizeNonCActivityWindowAndClearExamConfig() {
        Activity nonC = new Activity();
        nonC.setName("B activity");
        nonC.setLevel(Activity.Level.B1);
        nonC.setEnrollmentStart(LocalDateTime.now().minusHours(1));
        nonC.setEnrollmentEnd(LocalDateTime.now().plusHours(1));
        nonC.setExamStart(LocalDateTime.now().plusDays(1));
        nonC.setExamEnd(LocalDateTime.now().plusDays(1).plusHours(1));
        nonC.setExamPaperId(9L);
        nonC.setReviewerIds("[10]");
        nonC.setReviewerCount(1);
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activity result = activityService.create(nonC);

        assertEquals(result.getEnrollmentStart(), result.getMaterialStart());
        assertEquals(result.getEnrollmentEnd(), result.getMaterialEnd());
        assertEquals(false, result.getHasExam());
        assertEquals(null, result.getExamStart());
        assertEquals(null, result.getExamEnd());
        assertEquals(null, result.getExamPaperId());
    }

    @Test
    void getByReviewerId_shouldMatchExactIdsFromJson() {
        Activity exact = new Activity();
        exact.setReviewerIds("[1,10]");
        Activity notExact = new Activity();
        notExact.setReviewerIds("[10]");
        when(activityRepository.findAll()).thenReturn(List.of(exact, notExact));

        List<Activity> result = activityService.getByReviewerId(1L);

        assertEquals(List.of(exact), result);
    }
}
