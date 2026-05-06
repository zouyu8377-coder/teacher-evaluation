package com.school.teacherEval.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeacherLevel {
    NONE("无级别", -1),
    C("C级", 0),
    B("B级", 1),
    A("A级", 2);

    private final String displayName;
    private final int tier;

    public static TeacherLevel fromActivityLevel(Activity.Level activityLevel) {
        if (activityLevel == null) {
            return NONE;
        }
        return switch (activityLevel.getTier()) {
            case 0 -> C;
            case 1 -> B;
            case 2 -> A;
            default -> NONE;
        };
    }

    public boolean isHigherThan(TeacherLevel other) {
        if (other == null) {
            return this != NONE;
        }
        return this.tier > other.tier;
    }
}
