package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentEligibilityService {

    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;

    public boolean canEnroll(Long teacherId, Activity targetActivity) {
        Activity.Level targetLevel = targetActivity.getLevel();
        if (targetLevel == null) {
            return false;
        }

        if (targetLevel.getPrevLevels().isEmpty()) {
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
}
