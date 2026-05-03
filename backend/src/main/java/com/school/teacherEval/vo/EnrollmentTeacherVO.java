package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentTeacherVO {
    private Long id;
    private String username;
    private String realName;
    private String department;
    private LocalDateTime enrolledAt;
    private Long examRecordId;
    private LocalDateTime submittedAt;
    private String submissionStatus;
}
