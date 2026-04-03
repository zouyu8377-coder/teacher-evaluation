package com.school.teacherEval.service;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.repository.DocumentRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final MinioConfig minioConfig;
    private final EnrollmentService enrollmentService;
    
    public Page<Document> getDocuments(Long userId, Long periodId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (userId != null && periodId != null) {
            return documentRepository.findByUserIdAndPeriodId(userId, periodId, pageable);
        }
        if (userId != null) {
            return documentRepository.findByUserId(userId, pageable);
        }
        if (periodId != null) {
            return documentRepository.findByPeriodId(periodId, pageable);
        }
        
        return documentRepository.findAllActive(pageable);
    }
    
    public Document getDocumentById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        if (doc.getIsDeleted() == 1) {
            throw new RuntimeException("文档已删除");
        }
        return doc;
    }
    
    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId, Long periodId, 
                                    String title, String description) throws Exception {
        if (!enrollmentService.isEnrolled(periodId, userId)) {
            throw new RuntimeException("您尚未报名该考核周期，无法上传文档");
        }
        
        String bucketName = minioConfig.getBucketName();
        MinioClient minioClient = minioConfig.minioClient();
        
        // 创建 bucket（如果不存在）
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filePath = "documents/" + userId + "/" + UUID.randomUUID() + extension;
        
        // 上传到 MinIO
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filePath)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        
        // 保存记录到数据库
        Document document = new Document();
        document.setUserId(userId);
        document.setPeriodId(periodId);
        document.setTitle(title);
        document.setFilePath(filePath);
        document.setFileName(originalFilename);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setDescription(description);
        
        return documentRepository.save(document);
    }
    
    @Transactional
    public Document updateDocument(Long id, Long userId, String title, String description) {
        Document document = getDocumentById(id);
        
        // 验证权限
        if (!document.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此文档");
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
        
        // 验证权限：只有上传者本人或管理员可以删除
        if (!document.getUserId().equals(userId) && !role.equals("admin")) {
            throw new RuntimeException("无权限删除此文档");
        }
        
        // 软删除
        document.setIsDeleted(1);
        documentRepository.save(document);
        
        // 从 MinIO 删除文件
        try {
            minioConfig.minioClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(document.getFilePath())
                    .build());
        } catch (Exception e) {
            // 记录日志但不影响删除操作
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
}