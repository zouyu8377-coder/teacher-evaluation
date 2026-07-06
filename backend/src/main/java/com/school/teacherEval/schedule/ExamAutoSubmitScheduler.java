package com.school.teacherEval.schedule;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.ExamRecordRepository;
import com.school.teacherEval.service.ExamRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamAutoSubmitScheduler {

    private final ExamRecordRepository recordRepository;
    private final ActivityRepository activityRepository;
    private final ExamRecordService examRecordService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoSubmitExpiredExams() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRecord> inProgressRecords = recordRepository.findByStatus(ExamRecord.Status.in_progress);

        for (ExamRecord record : inProgressRecords) {
            try {
                Activity activity = activityRepository.findById(record.getActivityId()).orElse(null);
                if (activity == null || activity.getExamEnd() == null || !now.isAfter(activity.getExamEnd())) {
                    continue;
                }

                log.info("Exam expired without submission, marking zero score: recordId={}, activityId={}, teacherId={}",
                    record.getId(), record.getActivityId(), record.getTeacherId());
                examRecordService.markMissingSubmissionAsZero(activity, record.getTeacherId());
            } catch (Exception e) {
                log.error("Failed to mark expired exam as zero: recordId={}, error={}", record.getId(), e.getMessage());
            }
        }
    }
}
