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

    /**
     * 每分钟检查一次，提交已过期但未提交的试卷
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void autoSubmitExpiredExams() {
        LocalDateTime now = LocalDateTime.now();

        // 查找所有已过期但仍在进行中的考试记录
        List<ExamRecord> inProgressRecords = recordRepository.findByStatus(ExamRecord.Status.in_progress);

        for (ExamRecord record : inProgressRecords) {
            try {
                Activity activity = activityRepository.findById(record.getActivityId()).orElse(null);
                if (activity == null) continue;

                // 检查考试是否已结束
                if (activity.getExamEnd() != null && now.isAfter(activity.getExamEnd())) {
                    // 检查是否有作答（answers不为空）
                    if (record.getAnswers() != null && !record.getAnswers().isEmpty() &&
                        !record.getAnswers().equals("{}")) {
                        // 有作答，自动提交
                        log.info("自动提交过期试卷: recordId={}, activityId={}, teacherId={}",
                            record.getId(), record.getActivityId(), record.getTeacherId());
                        examRecordService.submitExam(record.getId(), record.getTeacherId());
                    } else {
                        // 未作答，将状态改为未提交
                        record.setStatus(ExamRecord.Status.not_submitted);
                        recordRepository.save(record);
                        log.info("标记未提交试卷: recordId={}, activityId={}, teacherId={}",
                            record.getId(), record.getActivityId(), record.getTeacherId());
                    }
                }
            } catch (Exception e) {
                log.error("自动提交试卷失败: recordId={}, error={}", record.getId(), e.getMessage());
            }
        }
    }
}