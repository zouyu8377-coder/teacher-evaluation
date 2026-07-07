package com.school.teacherEval.schedule;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.LearningMaterial;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.LearningMaterialRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityFileCleanupScheduler {

    private final ActivityRepository activityRepository;
    private final DocumentRepository documentRepository;
    private final LearningMaterialRepository materialRepository;
    private final MinioConfig minioConfig;

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupExpiredActivityFiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3);
        List<Long> expiredActivityIds = activityRepository.findAll().stream()
                .filter(activity -> {
                    LocalDateTime endTime = resolveActivityEndTime(activity);
                    return endTime != null && endTime.isBefore(cutoff);
                })
                .map(Activity::getId)
                .filter(Objects::nonNull)
                .toList();

        if (expiredActivityIds.isEmpty()) {
            return;
        }

        int documentCount = cleanupDocuments(expiredActivityIds);
        int materialCount = cleanupMaterials(expiredActivityIds);
        log.info("Cleaned expired activity files, activityCount={}, documentCount={}, materialCount={}",
                expiredActivityIds.size(), documentCount, materialCount);
    }

    private int cleanupDocuments(List<Long> activityIds) {
        List<Document> documents = documentRepository.findByActivityIdInAndIsDeleted(activityIds, 0);
        for (Document document : documents) {
            removeObject(document.getFilePath());
            document.setIsDeleted(1);
            documentRepository.save(document);
        }
        return documents.size();
    }

    private int cleanupMaterials(List<Long> activityIds) {
        List<LearningMaterial> materials = materialRepository.findByActivityIdInAndIsDeleted(activityIds, 0);
        for (LearningMaterial material : materials) {
            removeObject(material.getFilePath());
            material.setIsDeleted(1);
            materialRepository.save(material);
        }
        return materials.size();
    }

    private void removeObject(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            MinioClient minioClient = minioConfig.minioClient();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(filePath)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to remove expired file from MinIO: {}", filePath, e);
        }
    }

    private LocalDateTime resolveActivityEndTime(Activity activity) {
        if (Boolean.TRUE.equals(activity.getHasExam()) && activity.getExamEnd() != null) {
            return activity.getExamEnd();
        }
        if (activity.getMaterialEnd() != null) {
            return activity.getMaterialEnd();
        }
        if (activity.getEndDate() != null) {
            return activity.getEndDate().atTime(LocalTime.MAX);
        }
        return activity.getEnrollmentEnd();
    }
}
