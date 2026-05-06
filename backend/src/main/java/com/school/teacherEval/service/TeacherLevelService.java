package com.school.teacherEval.service;

import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.TeacherLevelHistory;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.TeacherLevelHistoryRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherLevelService {

    private final UserRepository userRepository;
    private final TeacherLevelHistoryRepository historyRepository;

    /**
     * 自动升级：仅在通过的等级高于当前等级时执行
     */
    @Transactional
    public void upgradeIfHigher(Long teacherId, Activity.Level passedActivityLevel) {
        User user = userRepository.findById(teacherId).orElse(null);
        if (user == null) {
            log.warn("Teacher not found for id={}", teacherId);
            return;
        }

        TeacherLevel newLevel = TeacherLevel.fromActivityLevel(passedActivityLevel);
        TeacherLevel currentLevel = user.getTeacherLevel();

        if (newLevel.isHigherThan(currentLevel)) {
            changeLevelInternal(user, newLevel, TeacherLevelHistory.ChangeType.AUTO, null);
            log.info("Auto-upgraded teacher {} from {} to {}", teacherId, currentLevel, newLevel);
        }
    }

    /**
     * 管理员手动变更等级（支持降级）
     */
    @Transactional
    public void changeLevel(Long teacherId, TeacherLevel newLevel, Long changedByUserId) {
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (user.getTeacherLevel() == newLevel) {
            return; // 无变化
        }

        changeLevelInternal(user, newLevel, TeacherLevelHistory.ChangeType.MANUAL, changedByUserId);
        log.info("Manual level change for teacher {} to {} by admin {}", teacherId, newLevel, changedByUserId);
    }

    public TeacherLevel getCurrentLevel(Long teacherId) {
        return userRepository.findById(teacherId)
                .map(User::getTeacherLevel)
                .orElse(TeacherLevel.NONE);
    }

    public LocalDateTime getLevelChangedAt(Long teacherId) {
        return userRepository.findById(teacherId)
                .map(User::getLevelChangedAt)
                .orElse(null);
    }

    public List<TeacherLevelHistory> getHistory(Long teacherId) {
        return historyRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    private void changeLevelInternal(User user, TeacherLevel newLevel,
                                     TeacherLevelHistory.ChangeType changeType, Long changedByUserId) {
        TeacherLevel oldLevel = user.getTeacherLevel();
        LocalDateTime now = LocalDateTime.now();

        user.setTeacherLevel(newLevel);
        user.setLevelChangedAt(now);
        userRepository.save(user);

        TeacherLevelHistory history = new TeacherLevelHistory();
        history.setTeacherId(user.getId());
        history.setOldLevel(oldLevel);
        history.setNewLevel(newLevel);
        history.setChangeType(changeType);
        history.setChangedByUserId(changedByUserId);
        historyRepository.save(history);
    }
}
