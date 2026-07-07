package com.school.teacherEval.service;
import com.school.teacherEval.exception.BusinessException;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import com.school.teacherEval.repository.EnrollmentRepository;
import com.school.teacherEval.repository.EvaluationRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    // 允许上传的文件扩展名白名单
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".zip"
    );
    // 允许上传的 MIME 类型白名单（仅作辅助校验，以扩展名为主）
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "image/jpeg", "image/png", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain", "application/zip"
    );
    // 最大文件大小 200MB
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final MinioConfig minioConfig;
    private final EnrollmentService enrollmentService;
    private final ActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EvaluationRepository evaluationRepository;
    
    public Page<Document> getDocuments(Long userId, Long activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (userId != null && activityId != null) {
            return documentRepository.findByUserIdAndActivityId(userId, activityId, pageable);
        }
        if (userId != null) {
            return documentRepository.findByUserId(userId, pageable);
        }
        if (activityId != null) {
            return documentRepository.findByActivityId(activityId, pageable);
        }
        
        return documentRepository.findAllActive(pageable);
    }
    
    public Document getDocumentById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        if (doc.getIsDeleted() == 1) {
            throw new BusinessException("文档已删除");
        }
        return doc;
    }

    public Document getDocumentById(Long id, User currentUser) {
        Document document = getDocumentById(id);
        assertCanAccessDocument(currentUser, document);
        return document;
    }
    
    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId, Long activityId,
                                    String title, String description) throws Exception {
        if (!enrollmentService.isEnrolledByActivity(activityId, userId)) {
            throw new BusinessException("您尚未报名该活动，无法上传文档");
        }
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException("活动不存在"));
        if (Boolean.TRUE.equals(activity.getHasExam())) {
            throw new BusinessException("考试类活动无需上传文档");
        }
        assertMaterialMutationOpen(activity);
        assertMaterialDraft(activityId, userId);

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制（最大200MB）");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        // 获取并校验扩展名
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件类型，仅允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // MIME 类型辅助校验（客户端可伪造，不能完全依赖）
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("文件 {} 的 MIME 类型 {} 不在白名单中，但仍允许上传（以扩展名校验为主）", originalFilename, contentType);
        }

        // 文件名安全检查：去除路径穿越字符，保留安全字符
        String safeFileName = originalFilename.replaceAll("[^\\w\\u4e00-\\u9fa5\\-\\.]", "_");
        if (safeFileName.length() > 200) {
            safeFileName = safeFileName.substring(0, 200);
        }

        String bucketName = minioConfig.getBucketName();
        MinioClient minioClient = minioConfig.minioClient();

        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 使用随机 UUID 作为对象名，避免文件名冲突和路径穿越
        String filePath = "documents/" + userId + "/" + UUID.randomUUID() + extension;

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filePath)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build());

        Document document = new Document();
        document.setUserId(userId);
        document.setActivityId(activityId);
        document.setTitle(title);
        document.setFilePath(filePath);
        document.setFileName(safeFileName);
        document.setFileSize(file.getSize());
        document.setFileType(contentType != null ? contentType : "application/octet-stream");
        document.setDescription(description);

        return documentRepository.save(document);
    }
    
    @Transactional
    public Document updateDocument(Long id, Long userId, String title, String description) {
        Document document = getDocumentById(id);
        
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此文档");
        }
        Activity activity = activityRepository.findById(document.getActivityId())
                .orElseThrow(() -> new BusinessException("活动不存在"));
        assertMaterialMutationOpen(activity);
        assertMaterialDraft(document.getActivityId(), userId);
        
        if (title != null) {
            document.setTitle(title);
        }
        if (description != null) {
            document.setDescription(description);
        }
        
        return documentRepository.save(document);
    }
    
    @Transactional
    public void deleteDocument(Long id, Long userId, String role) {
        Document document = getDocumentById(id);
        
        if (!document.getUserId().equals(userId) && !role.equals("admin")) {
            throw new BusinessException("无权限删除此文档");
        }
        if (document.getUserId().equals(userId) && !role.equals("admin")) {
            Activity activity = activityRepository.findById(document.getActivityId())
                    .orElseThrow(() -> new BusinessException("活动不存在"));
            assertMaterialMutationOpen(activity);
            assertMaterialDraft(document.getActivityId(), userId);
        }
        
        document.setIsDeleted(1);
        documentRepository.save(document);
        
        try {
            minioConfig.minioClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(document.getFilePath())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 删除文件失败, documentId={}, filePath={}", id, document.getFilePath(), e);
            // 不抛出异常：数据库已标记删除，对象存储文件可后续通过定时任务清理
        }
    }
    
    public InputStream downloadDocument(Long id, User currentUser) throws Exception {
        Document document = getDocumentById(id, currentUser);
        
        return minioConfig.minioClient().getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(document.getFilePath())
                .build());
    }
    
    public String getFileName(Long id) {
        Document document = getDocumentById(id);
        return document.getFileName();
    }

    private void assertCanAccessDocument(User currentUser, Document document) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied");
        }
        if (currentUser.getRole() == User.Role.teacher && !document.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public Optional<Document> getLatestDocument(Long userId, Long activityId) {
        return documentRepository.findByUserIdAndActivityId(userId, activityId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .findFirst();
    }

    @Transactional
    public PeriodEnrollment confirmMaterialSubmission(Long activityId, Long userId) {
        PeriodEnrollment enrollment = getActiveEnrollment(activityId, userId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException("活动不存在"));
        if (activity.getLevel() == Activity.Level.C || Boolean.TRUE.equals(activity.getHasExam())) {
            throw new BusinessException("考试类活动不需要确认材料");
        }
        if (documentRepository.findFirstByActivityIdAndUserId(activityId, userId).isEmpty()) {
            throw new BusinessException("请先上传至少一份材料");
        }
        enrollment.setMaterialStatus(PeriodEnrollment.MaterialStatus.submitted);
        enrollment.setMaterialSubmittedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public PeriodEnrollment cancelMaterialSubmission(Long activityId, Long userId) {
        PeriodEnrollment enrollment = getActiveEnrollment(activityId, userId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException("活动不存在"));
        LocalDateTime end = getMaterialEnd(activity);
        if (end != null && LocalDateTime.now().isAfter(end)) {
            throw new BusinessException("活动已截止，无法取消确认");
        }
        boolean hasEvaluation = evaluationRepository.findByActivityIdAndTeacherId(activityId, userId).stream()
                .anyMatch(e -> e.getScore() != null);
        if (hasEvaluation) {
            throw new BusinessException("评分员已评分，无法取消确认");
        }
        enrollment.setMaterialStatus(PeriodEnrollment.MaterialStatus.draft);
        enrollment.setMaterialSubmittedAt(null);
        return enrollmentRepository.save(enrollment);
    }

    public boolean isMaterialReviewable(Activity activity, PeriodEnrollment enrollment, boolean hasDocument) {
        if (activity == null || enrollment == null || !hasDocument) {
            return false;
        }
        PeriodEnrollment.MaterialStatus status = enrollment.getMaterialStatus();
        if (status == PeriodEnrollment.MaterialStatus.submitted || status == PeriodEnrollment.MaterialStatus.auto_submitted) {
            return true;
        }
        LocalDateTime end = getMaterialEnd(activity);
        return end != null && !LocalDateTime.now().isBefore(end);
    }

    @Transactional
    public PeriodEnrollment autoConfirmIfExpired(Activity activity, PeriodEnrollment enrollment) {
        if (activity == null || enrollment == null) {
            return enrollment;
        }
        boolean hasDocument = documentRepository.findFirstByActivityIdAndUserId(activity.getId(), enrollment.getTeacherId()).isPresent();
        if (!hasDocument || !isMaterialReviewable(activity, enrollment, true)) {
            return enrollment;
        }
        PeriodEnrollment.MaterialStatus status = enrollment.getMaterialStatus();
        if (status == PeriodEnrollment.MaterialStatus.submitted || status == PeriodEnrollment.MaterialStatus.auto_submitted) {
            return enrollment;
        }
        enrollment.setMaterialStatus(PeriodEnrollment.MaterialStatus.auto_submitted);
        enrollment.setMaterialSubmittedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    public LocalDateTime getMaterialEnd(Activity activity) {
        if (activity == null) {
            return null;
        }
        return activity.getMaterialEnd() != null ? activity.getMaterialEnd() : activity.getEnrollmentEnd();
    }

    private PeriodEnrollment getActiveEnrollment(Long activityId, Long userId) {
        PeriodEnrollment enrollment = enrollmentRepository.findByActivityIdAndTeacherId(activityId, userId)
                .orElseThrow(() -> new BusinessException("未找到报名记录"));
        if (enrollment.getStatus() != PeriodEnrollment.Status.enrolled) {
            throw new BusinessException("报名记录已失效");
        }
        return enrollment;
    }

    private void assertMaterialDraft(Long activityId, Long userId) {
        PeriodEnrollment enrollment = getActiveEnrollment(activityId, userId);
        PeriodEnrollment.MaterialStatus status = enrollment.getMaterialStatus();
        if (status == PeriodEnrollment.MaterialStatus.submitted || status == PeriodEnrollment.MaterialStatus.auto_submitted) {
            throw new BusinessException("材料已确认提交，如需修改请先取消确认");
        }
    }

    private void assertMaterialMutationOpen(Activity activity) {
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (Boolean.TRUE.equals(activity.getHasExam()) || activity.getLevel() == Activity.Level.C) {
            throw new BusinessException("考试类活动无需上传文档");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = activity.getMaterialStart() != null ? activity.getMaterialStart() : activity.getEnrollmentStart();
        LocalDateTime end = activity.getMaterialEnd() != null ? activity.getMaterialEnd() : activity.getEnrollmentEnd();
        if (start != null && now.isBefore(start)) {
            throw new BusinessException("材料提交尚未开始");
        }
        if (end != null && now.isAfter(end)) {
            throw new BusinessException("材料提交已结束");
        }
    }
}
