package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.TeacherLevelHistoryRepository;
import com.school.teacherEval.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherLevelServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeacherLevelHistoryRepository historyRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private TeacherLevelService teacherLevelService;

    @Test
    void upgradeIfHigher_shouldRemoveObsoleteUnfinishedEnrollments() {
        User teacher = new User();
        teacher.setId(1L);
        teacher.setTeacherLevel(TeacherLevel.C);

        PeriodEnrollment sameTierUnfinished = enrollment(10L);
        PeriodEnrollment nextTierUnfinished = enrollment(11L);
        PeriodEnrollment sameTierPublished = enrollment(12L);

        Activity bUnpublished = activity(10L, Activity.Level.B2, false);
        Activity aUnpublished = activity(11L, Activity.Level.A2, false);
        Activity bPublished = activity(12L, Activity.Level.B1, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findByTeacherId(1L))
                .thenReturn(List.of(sameTierUnfinished, nextTierUnfinished, sameTierPublished));
        when(activityRepository.findById(10L)).thenReturn(Optional.of(bUnpublished));
        when(activityRepository.findById(11L)).thenReturn(Optional.of(aUnpublished));
        when(activityRepository.findById(12L)).thenReturn(Optional.of(bPublished));

        teacherLevelService.upgradeIfHigher(1L, Activity.Level.B2);

        assertEquals(TeacherLevel.B, teacher.getTeacherLevel());
        assertEquals(PeriodEnrollment.Status.removed, sameTierUnfinished.getStatus());
        assertEquals(PeriodEnrollment.Status.enrolled, nextTierUnfinished.getStatus());
        assertEquals(PeriodEnrollment.Status.enrolled, sameTierPublished.getStatus());
        verify(enrollmentRepository).save(sameTierUnfinished);
        verify(enrollmentRepository, never()).save(nextTierUnfinished);
        verify(enrollmentRepository, never()).save(sameTierPublished);
        verify(userRepository).save(teacher);
        verify(historyRepository).save(any());
    }

    private PeriodEnrollment enrollment(Long activityId) {
        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setActivityId(activityId);
        enrollment.setTeacherId(1L);
        enrollment.setStatus(PeriodEnrollment.Status.enrolled);
        return enrollment;
    }

    private Activity activity(Long id, Activity.Level level, boolean scoresPublished) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setLevel(level);
        activity.setScoresPublished(scoresPublished);
        return activity;
    }
}
