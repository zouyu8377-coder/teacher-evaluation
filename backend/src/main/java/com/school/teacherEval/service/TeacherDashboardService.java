package com.school.teacherEval.service;

import com.school.teacherEval.dto.TeacherDashboardDTO;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.ExamRecord;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import com.school.teacherEval.repository.ExamRecordRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherDashboardService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ActivityRepository activityRepository;
    private final ExamRecordRepository examRecordRepository;

    public TeacherDashboardDTO getDashboard(Long teacherId) {
        TeacherDashboardDTO dto = new TeacherDashboardDTO();

        // 1. 用户基本信息
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        dto.setUserInfo(toUserInfo(user));

        // 2. 当前级别信息
        dto.setCurrentLevel(getCurrentLevelInfo(teacherId));

        // 3. 当前报考/进行中的考核
        dto.setCurrentEnrollments(getCurrentEnrollments(teacherId));

        // 4. 历史考核记录
        dto.setHistoryRecords(getHistoryRecords(teacherId));

        // 5. 待办事项
        dto.setTodoItems(getTodoItems(teacherId));

        return dto;
    }

    private TeacherDashboardDTO.UserInfo toUserInfo(User user) {
        TeacherDashboardDTO.UserInfo info = new TeacherDashboardDTO.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setDepartment(user.getDepartment());
        info.setRole(user.getRole().name());
        info.setStatus(user.getStatus());
        info.setCreatedAt(user.getCreatedAt());
        return info;
    }

    /**
     * 获取当前级别信息 - 基于已通过的最高级别
     * 未通过C级时视为无级别
     */
    private TeacherDashboardDTO.LevelInfo getCurrentLevelInfo(Long teacherId) {
        TeacherDashboardDTO.LevelInfo levelInfo = new TeacherDashboardDTO.LevelInfo();

        // 查找教师已发布且及格的成绩
        List<Evaluation> passedEvals = evaluationRepository
                .findByTeacherIdAndIsPublished(teacherId)
                .stream()
                .filter(e -> e.getFinalScore() != null && e.getFinalScore().compareTo(new BigDecimal("60")) >= 0)
                .collect(Collectors.toList());

        if (passedEvals.isEmpty()) {
            // 未通过任何考核，视为无级别
            levelInfo.setLevel(null);
            levelInfo.setLevelName("无级别");
            levelInfo.setHasPassed(false);
            levelInfo.setNextLevel("C");
            levelInfo.setCanEnrollNext(true);
            return levelInfo;
        }

        // 找出已通过的最高级别
        Activity.Level highestLevel = null;
        BigDecimal bestScore = BigDecimal.ZERO;
        LocalDateTime passedAt = null;

        for (Evaluation eval : passedEvals) {
            Activity activity = activityRepository.findById(eval.getActivityId()).orElse(null);
            if (activity == null) continue;

            Activity.Level level = activity.getLevel();
            if (highestLevel == null || level.getTier() > highestLevel.getTier()) {
                highestLevel = level;
                bestScore = eval.getFinalScore();
                passedAt = eval.getUpdatedAt();
            } else if (level.getTier() == highestLevel.getTier() &&
                       eval.getFinalScore().compareTo(bestScore) > 0) {
                bestScore = eval.getFinalScore();
                passedAt = eval.getUpdatedAt();
            }
        }

        if (highestLevel == null) {
            highestLevel = Activity.Level.C;
        }

        levelInfo.setLevel(highestLevel.name());
        levelInfo.setLevelName(highestLevel.getDisplayName());
        levelInfo.setHasPassed(true);
        levelInfo.setBestScore(bestScore);
        levelInfo.setPassedAt(passedAt);

        // 计算下一级（可能多个）
        List<Activity.Level> nextLevels = Activity.Level.getNextLevels(highestLevel);
        if (!nextLevels.isEmpty()) {
            // 返回可报考的下一级别名称（多个用逗号分隔）
            String nextLevelNames = nextLevels.stream()
                    .map(Activity.Level::name)
                    .collect(Collectors.joining(","));
            levelInfo.setNextLevel(nextLevelNames);
            levelInfo.setCanEnrollNext(true);
        } else {
            levelInfo.setNextLevel("A1");
            levelInfo.setCanEnrollNext(false); // 已是最高级
        }

        return levelInfo;
    }

    /**
     * 获取当前进行中的报考
     */
    private List<TeacherDashboardDTO.EnrollmentInfo> getCurrentEnrollments(Long teacherId) {
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByTeacherId(teacherId);

        // 获取所有已发布评分
        List<Evaluation> publishedEvals = evaluationRepository.findByTeacherIdAndIsPublished(teacherId);

        return enrollments.stream()
                .filter(e -> e.getStatus() == PeriodEnrollment.Status.enrolled)
                .map(e -> {
                    Activity activity = activityRepository.findById(e.getActivityId()).orElse(null);
                    if (activity == null) return null;

                    TeacherDashboardDTO.EnrollmentInfo info = new TeacherDashboardDTO.EnrollmentInfo();
                    info.setEnrollmentId(e.getId());
                    info.setActivityId(activity.getId());
                    info.setActivityName(activity.getName());
                    info.setLevel(activity.getLevel().name());
                    info.setLevelName(activity.getLevel().getDisplayName());
                    info.setStatus(e.getStatus().name());
                    info.setEnrolledAt(e.getEnrolledAt());
                    info.setHasExam(activity.getHasExam());
                    info.setExamDurationMinutes(activity.getExamDurationMinutes());

                    // 设置活动时间（考试时间或材料上传时间）
                    if (Boolean.TRUE.equals(activity.getHasExam())) {
                        info.setExamStartTime(activity.getExamStart());
                        info.setExamEndTime(activity.getExamEnd());
                    } else {
                        info.setMaterialStartTime(activity.getMaterialStart());
                        info.setMaterialEndTime(activity.getMaterialEnd());
                    }

                    // 检查考试状态
                    if (Boolean.TRUE.equals(activity.getHasExam())) {
                        List<ExamRecord> examRecords = examRecordRepository
                                .findByTeacherIdAndActivityId(teacherId, activity.getId());
                        ExamRecord examRecord = examRecords.isEmpty() ? null : examRecords.get(0);

                        if (examRecord == null) {
                            info.setExamStatus("not_started");
                        } else if (Boolean.TRUE.equals(examRecord.getIsSubmitted())) {
                            info.setExamStatus("completed");
                            info.setExamRecordId(examRecord.getId());
                            info.setCorrectCount(examRecord.getCorrectCount());
                            info.setWrongCount(examRecord.getWrongCount());
                        } else {
                            info.setExamStatus("in_progress");
                            info.setExamStartTime(examRecord.getStartedAt());
                            info.setExamEndTime(examRecord.getUpdatedAt());
                            info.setExamRecordId(examRecord.getId());
                        }
                    }

                    // 检查评分状态
                    Optional<Evaluation> evalOpt = publishedEvals.stream()
                            .filter(ev -> ev.getActivityId().equals(activity.getId()))
                            .findFirst();
                    if (evalOpt.isPresent()) {
                        Evaluation eval = evalOpt.get();
                        info.setScorePublished(true);
                        info.setFinalScore(eval.getFinalScore());
                        info.setIsPassed(eval.getFinalScore() != null && eval.getFinalScore().compareTo(new BigDecimal("60")) >= 0);
                    } else {
                        info.setScorePublished(false);
                        info.setFinalScore(null);
                        info.setIsPassed(null);
                    }

                    return info;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取历史考核记录（已完成的）
     */
    private List<TeacherDashboardDTO.HistoryRecord> getHistoryRecords(Long teacherId) {
        // 获取所有报名记录
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByTeacherId(teacherId);
        List<Long> activityIds = enrollments.stream()
                .map(PeriodEnrollment::getActivityId)
                .collect(Collectors.toList());

        // 获取这些活动的评分记录
        List<Evaluation> evaluations = evaluationRepository.findByTeacherId(teacherId)
                .stream()
                .filter(e -> activityIds.contains(e.getActivityId()))
                .collect(Collectors.toList());

        // 按活动分组，取最新的记录
        Map<Long, Evaluation> latestByActivity = evaluations.stream()
                .collect(Collectors.toMap(
                        Evaluation::getActivityId,
                        e -> e,
                        (existing, replacement) -> existing.getUpdatedAt().isAfter(replacement.getUpdatedAt()) ? existing : replacement
                ));

        List<TeacherDashboardDTO.HistoryRecord> records = new ArrayList<>();
        for (Map.Entry<Long, Evaluation> entry : latestByActivity.entrySet()) {
            Activity activity = activityRepository.findById(entry.getKey()).orElse(null);
            if (activity == null) continue;

            Evaluation eval = entry.getValue();
            if (!Boolean.TRUE.equals(eval.getIsPublished())) continue; // 只显示已发布的

            TeacherDashboardDTO.HistoryRecord record = new TeacherDashboardDTO.HistoryRecord();
            record.setActivityId(activity.getId());
            record.setActivityName(activity.getName());
            record.setLevel(activity.getLevel().name());
            record.setLevelName(activity.getLevel().getDisplayName());
            record.setFinalScore(eval.getFinalScore());
            record.setIsPassed(eval.getFinalScore() != null && eval.getFinalScore().compareTo(new BigDecimal("60")) >= 0);

            // 找到报名时间
            Optional<PeriodEnrollment> enrollment = enrollments.stream()
                    .filter(e -> e.getActivityId().equals(activity.getId()))
                    .findFirst();
            enrollment.ifPresent(e -> record.setEnrolledAt(e.getEnrolledAt()));

            record.setCompletedAt(eval.getUpdatedAt());

            // 状态
            if (Boolean.TRUE.equals(eval.getIsLocked())) {
                record.setStatus("completed");
            } else if (eval.getStatus() == Evaluation.Status.submitted) {
                record.setStatus("evaluated");
            } else {
                record.setStatus("enrolled");
            }

            records.add(record);
        }

        // 按完成时间倒序
        records.sort((a, b) -> {
            if (a.getCompletedAt() == null) return 1;
            if (b.getCompletedAt() == null) return -1;
            return b.getCompletedAt().compareTo(a.getCompletedAt());
        });

        return records;
    }

    /**
     * 获取待办事项
     */
    private List<TeacherDashboardDTO.TodoItem> getTodoItems(Long teacherId) {
        List<TeacherDashboardDTO.TodoItem> todos = new ArrayList<>();

        // 检查当前进行中的报考
        List<PeriodEnrollment> enrollments = enrollmentRepository.findByTeacherId(teacherId);
        for (PeriodEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) continue;

            Activity activity = activityRepository.findById(enrollment.getActivityId()).orElse(null);
            if (activity == null) continue;

            // 需要参加考试
            if (Boolean.TRUE.equals(activity.getHasExam())) {
                List<ExamRecord> examRecordList = examRecordRepository
                        .findByTeacherIdAndActivityId(teacherId, activity.getId());
                ExamRecord examRecord = examRecordList.isEmpty() ? null : examRecordList.get(0);

                if (examRecord == null || !Boolean.TRUE.equals(examRecord.getIsSubmitted())) {
                    TeacherDashboardDTO.TodoItem todo = new TeacherDashboardDTO.TodoItem();
                    todo.setType("exam");
                    todo.setTitle("待完成考试");
                    todo.setDescription(activity.getName() + " - 考试时长 " + activity.getExamDurationMinutes() + " 分钟");
                    todo.setRelatedId(activity.getId());
                    todo.setActionUrl("/teacher/exam/" + activity.getId());
                    // 考试截止时间
                    if (activity.getEndDate() != null) {
                        todo.setDeadline(activity.getEndDate().atTime(23, 59, 59));
                    }
                    todos.add(todo);
                }
            }
        }

        // 检查是否有可报名的活动
        List<Activity> allActive = activityRepository.findByStatus(Activity.Status.active);
        for (Activity activity : allActive) {
            boolean alreadyEnrolled = enrollments.stream()
                    .anyMatch(e -> e.getActivityId().equals(activity.getId()) &&
                                   e.getStatus() == PeriodEnrollment.Status.enrolled);

            if (!alreadyEnrolled) {
                // 检查是否可以报名
                // 只有已通过对应级别才能报名下一级别
                // C级：未通过C级或无级别都可以报名
                // B2：已通过C级才能报名
                // B1：已通过B2才能报名
                // A2：已通过B1才能报名
                // A1：已通过A2才能报名
                Activity.Level currentLevel = getTeacherCurrentLevel(teacherId);
                Activity.Level requiredLevel = Activity.Level.getPrevLevel(activity.getLevel());

                boolean canEnroll = false;
                if (activity.getLevel() == Activity.Level.C) {
                    // C级：未通过任何级别（或无级别）都可以报名
                    canEnroll = currentLevel == null || currentLevel == Activity.Level.C;
                } else if (requiredLevel != null) {
                    // 其他级别：必须已通过前置级别
                    canEnroll = currentLevel != null && currentLevel.getTier() >= requiredLevel.getTier();
                }

                if (canEnroll) {
                    TeacherDashboardDTO.TodoItem todo = new TeacherDashboardDTO.TodoItem();
                    todo.setType("enrollment");
                    todo.setTitle("可报名考核");
                    todo.setDescription(activity.getName() + " (" + activity.getLevel().getDisplayName() + ")");
                    todo.setRelatedId(activity.getId());
                    todo.setActionUrl("/teacher/activities/" + activity.getId());
                    todo.setDeadline(activity.getEnrollmentEnd());
                    todos.add(todo);
                    break; // 只显示一个可报名的
                }
            }
        }

        return todos;
    }

    private Activity.Level getTeacherCurrentLevel(Long teacherId) {
        List<Evaluation> passedEvals = evaluationRepository
                .findByTeacherIdAndIsPublished(teacherId)
                .stream()
                .filter(e -> e.getFinalScore() != null && e.getFinalScore().compareTo(new BigDecimal("60")) >= 0)
                .collect(Collectors.toList());

        Activity.Level highestLevel = null;
        for (Evaluation eval : passedEvals) {
            Activity activity = activityRepository.findById(eval.getActivityId()).orElse(null);
            if (activity == null) continue;

            if (highestLevel == null || activity.getLevel().getOrder() > highestLevel.getOrder()) {
                highestLevel = activity.getLevel();
            }
        }

        // 未通过任何考核返回null（视为无级别）
        return highestLevel;
    }
}