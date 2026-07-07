package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnrollmentEligibilityService {

    private final UserRepository userRepository;

    public boolean canEnroll(Long teacherId, Activity targetActivity) {
        if (targetActivity == null) {
            return false;
        }
        Activity.Level targetLevel = targetActivity.getLevel();
        if (targetLevel == null) {
            return false;
        }

        TeacherLevel currentLevel = getCurrentTeacherLevel(teacherId);
        TeacherLevel targetTeacherLevel = TeacherLevel.fromActivityLevel(targetLevel);
        if (targetTeacherLevel.getTier() <= currentLevel.getTier()) {
            return false;
        }
        return targetTeacherLevel.getTier() == currentLevel.getTier() + 1;
    }

    public boolean isAboveCurrentLevel(Long teacherId, Activity activity) {
        if (activity == null || activity.getLevel() == null) {
            return false;
        }
        TeacherLevel currentLevel = getCurrentTeacherLevel(teacherId);
        TeacherLevel activityLevel = TeacherLevel.fromActivityLevel(activity.getLevel());
        return activityLevel.getTier() > currentLevel.getTier();
    }

    private TeacherLevel getCurrentTeacherLevel(Long teacherId) {
        return Optional.ofNullable(userRepository.findById(teacherId))
                .flatMap(optional -> optional)
                .map(User::getTeacherLevel)
                .filter(level -> level != null)
                .orElse(TeacherLevel.NONE);
    }
}
