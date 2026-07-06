package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentEligibilityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentEligibilityService service;

    @Test
    void canEnroll_shouldAllowCLevelWithoutPreviousLevel() {
        Activity activity = new Activity();
        activity.setLevel(Activity.Level.C);

        assertTrue(service.canEnroll(1L, activity));
    }

    @Test
    void canEnroll_shouldAllowB_whenTeacherHasCLevel() {
        Activity activity = new Activity();
        activity.setLevel(Activity.Level.B2);
        User teacher = new User();
        teacher.setTeacherLevel(TeacherLevel.C);
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));

        assertTrue(service.canEnroll(1L, activity));
    }

    @Test
    void canEnroll_shouldRejectA_whenTeacherOnlyHasCLevel() {
        Activity activity = new Activity();
        activity.setLevel(Activity.Level.A2);
        User teacher = new User();
        teacher.setTeacherLevel(TeacherLevel.C);
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findByTeacherId(1L)).thenReturn(java.util.List.of());

        assertFalse(service.canEnroll(1L, activity));
    }
}
