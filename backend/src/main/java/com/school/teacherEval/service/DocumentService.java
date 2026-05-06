package com.school.teacherEval.service;
import com.school.teacherEval.exception.BusinessException;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.repository.DocumentRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
    // 最大文件大小 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final MinioConfig minioConfig;
    private final EnrollmentService enrollmentService;
    
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
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        if (doc.getIsDeleted() == 1) {
            throw new BusinessException("文档已删除");
        }
        return doc;
    }
    
    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId, Long activityId,
                                    String title, String description) throws Exception {
        if (!enrollmentService.isEnrolledByActivity(activityId, userId)) {
            throw new BusinessException("您尚未报名该活动，无法上传文档");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制（最大50MB）");
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
    
    public InputStream downloadDocument(Long id) throws Exception {
        Document document = getDocumentById(id);
        
        return minioConfig.minioClient().getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(document.getFilePath())
                .build());
    }
    
    public String getFileName(Long id) {
        Document document = getDocumentById(id);
        return document.getFileName();
    }

    public Optional<Document> getLatestDocument(Long userId, Long activityId) {
        return documentRepository.findByUserIdAndActivityId(userId, activityId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .findFirst();
    }
}