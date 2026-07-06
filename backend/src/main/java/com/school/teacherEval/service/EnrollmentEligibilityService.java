package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentEligibilityService {

    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;

    public boolean canEnroll(Long teacherId, Activity targetActivity) {
        Activity.Level targetLevel = targetActivity.getLevel();
        if (targetLevel == null) {
            return false;
        }

        if (targetLevel.getPrevLevels().isEmpty()) {
            return true;
        }

        if (hasRequiredPersistedLevel(teacherId, targetLevel)) {
            return true;
        }

        List<PeriodEnrollment> enrollments = enrollmentRepository.findByTeacherId(teacherId);
        for (PeriodEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) {
                continue;
            }

            Activity previousActivity = activityRepository.findById(enrollment.getActivityId()).orElse(null);
            if (previousActivity == null || !Activity.Level.canProgressTo(previousActivity.getLevel(), targetLevel)) {
                continue;
            }

            boolean passed = evaluationRepository
                    .findByTeacherIdAndActivityId(teacherId, enrollment.getActivityId())
                    .stream()
                    .anyMatch(eval -> eval.getStatus() == Evaluation.Status.submitted
                            && Boolean.TRUE.equals(eval.getIsPublished())
                            && Boolean.TRUE.equals(eval.getIsPassed()));
            if (passed) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRequiredPersistedLevel(Long teacherId, Activity.Level targetLevel) {
        User user = userRepository.findById(teacherId).orElse(null);
        if (user == null || user.getTeacherLevel() == null || user.getTeacherLevel() == TeacherLevel.NONE) {
            return false;
        }
        TeacherLevel currentLevel = user.getTeacherLevel();
        return currentLevel.getTier() == targetLevel.getTier() - 1;
    }
}
