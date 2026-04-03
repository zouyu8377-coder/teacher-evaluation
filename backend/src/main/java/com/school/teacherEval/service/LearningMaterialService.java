package com.school.teacherEval.service;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.LearningMaterial;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningMaterialService {
    
    private final LearningMaterialRepository materialRepository;
    private final MinioConfig minioConfig;
    private final EnrollmentService enrollmentService;
    
    public Page<LearningMaterial> getMaterials(Long periodId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (periodId != null) {
            return materialRepository.findByPeriodId(periodId, pageable);
        }
        
        return materialRepository.findAllActive(pageable);
    }
    
    public LearningMaterial getMaterialById(Long id) {
        LearningMaterial material = materialRepository.findActiveById(id);
        if (material == null) {
            throw new RuntimeException("资料不存在");
        }
        return material;
    }
    
    @Transactional
    public LearningMaterial uploadMaterial(MultipartFile file, Long periodId, 
                                           String title, String description, Long createdBy) throws Exception {
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
        material.setPeriodId(periodId);
        material.setTitle(title);
        material.setFilePath(filePath);
        material.setFileName(originalFilename);
        material.setFileSize(file.getSize());
        material.setFileType(file.getContentType());
        material.setDescription(description);
        material.setCreatedBy(createdBy);
        
        return materialRepository.save(material);
    }
    
    @Transactional
    public LearningMaterial updateMaterial(Long id, Long currentUserId, String role,
                                            String title, String description) {
        LearningMaterial material = getMaterialById(id);
        
        if (!material.getCreatedBy().equals(currentUserId) && !role.equals("admin")) {
            throw new RuntimeException("无权限修改此资料");
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
            throw new RuntimeException("无权限删除此资料");
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
            if (!enrollmentService.isEnrolled(material.getPeriodId(), userId)) {
                throw new RuntimeException("您尚未报名该考核周期，无法下载学习资料");
            }
        }
        
        return minioConfig.minioClient().getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(material.getFilePath())
                .build());
    }
}