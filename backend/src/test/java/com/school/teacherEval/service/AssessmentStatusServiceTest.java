package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.vo.AssessmentStatusVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentStatusServiceTest {

    private final AssessmentStatusService service = new AssessmentStatusService();

    @Test
    void evaluate_shouldReturnExamOpen_whenEnrollmentAndExamWindowOpen() {
        Activity activity = cActivity(LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(50));
        PeriodEnrollment enrollment = enrollment();

        AssessmentStatusVO status = service.evaluate(activity, enrollment, null, null, false, null);

        assertEquals("exam_open", status.getBusinessStatus());
        assertTrue(status.getAvailableActions().contains("start_exam"));
    }

    @Test
    void evaluate_shouldReturnWaitPublish_whenExamSubmitted() {
        Activity activity = cActivity(LocalDateTime.now().minusHours(2), LocalDateTime.now().plusHours(1));
        PeriodEnrollment enrollment = enrollment();
        ExamRecord record = new ExamRecord();
        record.setStatus(ExamRecord.Status.submitted);

        AssessmentStatusVO status = service.evaluate(activity, enrollment, record, null, false, null);

        assertEquals("exam_submitted_wait_publish", status.getBusinessStatus());
        assertEquals("考试完成，等待成绩发布", status.getStatusText());
    }

    @Test
    void evaluate_shouldReturnPublishedPassed_whenScorePublished() {
        AssessmentStatusVO status = service.evaluate(new Activity(), enrollment(), null, null, true, true);

        assertEquals("published_passed", status.getBusinessStatus());
        assertTrue(status.getAvailableActions().contains("view_result"));
    }

    private Activity cActivity(LocalDateTime examStart, LocalDateTime examEnd) {
        Activity activity = new Activity();
        activity.setLevel(Activity.Level.C);
        activity.setHasExam(true);
        activity.setExamStart(examStart);
        activity.setExamEnd(examEnd);
        return activity;
    }

    private PeriodEnrollment enrollment() {
        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setStatus(PeriodEnrollment.Status.enrolled);
        return enrollment;
    }
}
