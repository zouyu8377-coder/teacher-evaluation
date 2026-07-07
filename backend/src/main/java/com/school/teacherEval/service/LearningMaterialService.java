package com.school.teacherEval.service;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.LearningMaterial;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.LearningMaterialRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningMaterialService {

    private static final long MAX_VIDEO_FILE_SIZE = 1024L * 1024 * 1024;
    private static final long MAX_NON_VIDEO_FILE_SIZE = 200L * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx", ".txt", ".zip", ".rar", ".mp4"
    );
    
    private final LearningMaterialRepository materialRepository;
    private final MinioConfig minioConfig;
    private final EnrollmentService enrollmentService;
    
    public Page<LearningMaterial> getMaterials(Long activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (activityId != null) {
            return materialRepository.findByActivityId(activityId, pageable);
        }
        
        return materialRepository.findAllActive(pageable);
    }
    
    public LearningMaterial getMaterialById(Long id) {
        LearningMaterial material = materialRepository.findActiveById(id);
        if (material == null) {
            throw new BusinessException("资料不存在");
        }
        return material;
    }
    
    @Transactional
    public LearningMaterial uploadMaterial(MultipartFile file, Long activityId, 
                                           String title, String description, Long createdBy) throws Exception {
        validateFile(file);

        String bucketName = minioConfig.getBucketName();
        MinioClient minioClient = minioConfig.minioClient();
        
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filePath = "materials/" + createdBy + "/" + UUID.randomUUID() + extension;
        
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filePath)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        
        LearningMaterial material = new LearningMaterial();
        material.setActivityId(activityId);
        material.setTitle(title);
        material.setFilePath(filePath);
        material.setFileName(originalFilename);
        material.setFileSize(file.getSize());
        material.setFileType(file.getContentType());
        material.setDescription(description);
        material.setCreatedBy(createdBy);
        
        return materialRepository.save(material);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的学习资料");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == originalFilename.length() - 1) {
            throw new BusinessException("文件缺少扩展名");
        }
        String extension = originalFilename.substring(lastDotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的学习资料类型，仅允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        long maxSize = ".mp4".equals(extension) ? MAX_VIDEO_FILE_SIZE : MAX_NON_VIDEO_FILE_SIZE;
        if (file.getSize() > maxSize) {
            throw new BusinessException(".mp4".equals(extension)
                    ? "视频资料单个文件不能超过1GB"
                    : "学习资料单个文件不能超过200MB");
        }
    }
    
    @Transactional
    public LearningMaterial updateMaterial(Long id, Long currentUserId, String role,
                                            String title, String description) {
        LearningMaterial material = getMaterialById(id);
        
        if (!material.getCreatedBy().equals(currentUserId) && !role.equals("admin")) {
            throw new BusinessException("无权限修改此资料");
        }
        
        if (title != null) {
            material.setTitle(title);
        }
        if (description != null) {
            material.setDescription(description);
        }
        
        return materialRepository.save(material);
    }
    
    @Transactional
    public void deleteMaterial(Long id, Long currentUserId, String role) {
        LearningMaterial material = getMaterialById(id);
        
        if (!material.getCreatedBy().equals(currentUserId) && !role.equals("admin")) {
            throw new BusinessException("无权限删除此资料");
        }
        
        material.setIsDeleted(1);
        materialRepository.save(material);
        
        try {
            minioConfig.minioClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(material.getFilePath())
                    .build());
        } catch (Exception e) {
        }
    }
    
    public InputStream downloadMaterial(Long id, Long userId, String role) throws Exception {
        LearningMaterial material = getMaterialById(id);
        
        if (!role.equals("admin") && !role.equals("evaluator")) {
            if (material.getActivityId() != null) {
                boolean isEnrolled = enrollmentService.isEnrolledByActivity(material.getActivityId(), userId);
                if (!isEnrolled) {
                    throw new BusinessException("您尚未报名该活动，无法下载学习资料");
                }
            }
        }
        
        return minioConfig.minioClient().getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(material.getFilePath())
                .build());
    }
}
