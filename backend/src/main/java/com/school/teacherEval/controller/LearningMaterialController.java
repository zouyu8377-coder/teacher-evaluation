package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.LearningMaterial;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.ActivityService;
import com.school.teacherEval.service.LearningMaterialService;
import com.school.teacherEval.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learning-materials")
@RequiredArgsConstructor
@Tag(name = "学习资料管理")
public class LearningMaterialController {
    
    private final LearningMaterialService materialService;
    private final UserService userService;
    private final ActivityService activityService;
    
    @GetMapping
    @Operation(summary = "获取学习资料列表")
    public ApiResponse<Map<String, Object>> getMaterials(
            @RequestParam(required = false) Long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<LearningMaterial> materialPage = materialService.getMaterials(activityId, page, size);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", materialPage.getContent().stream().map(this::toVO).collect(Collectors.toList()));
        data.put("total", materialPage.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        
        return ApiResponse.success(data);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取学习资料详情")
    public ApiResponse<Map<String, Object>> getMaterial(@PathVariable Long id) {
        LearningMaterial material = materialService.getMaterialById(id);
        return ApiResponse.success(toVO(material));
    }
    
    @PostMapping
    @Operation(summary = "上传学习资料")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<LearningMaterial> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("activityId") Long activityId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) throws Exception {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        LearningMaterial material = materialService.uploadMaterial(
                file, activityId, title, description, currentUser.getId());
        
        return ApiResponse.success(material);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新学习资料")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<LearningMaterial> updateMaterial(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        LearningMaterial material = materialService.updateMaterial(
                id, currentUser.getId(), currentUser.getRole().name(), title, description);
        
        return ApiResponse.success(material);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学习资料")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        materialService.deleteMaterial(id, currentUser.getId(), currentUser.getRole().name());
        return ApiResponse.success("删除成功", null);
    }
    
    @GetMapping("/{id}/download")
    @Operation(summary = "下载学习资料")
    public void downloadMaterial(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        LearningMaterial material = materialService.getMaterialById(id);
        String fileName = material.getFileName();
        
        response.setContentType(material.getFileType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        
        InputStream inputStream = materialService.downloadMaterial(id, currentUser.getId(), currentUser.getRole().name());
        OutputStream outputStream = response.getOutputStream();
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        
        inputStream.close();
        outputStream.flush();
    }
    
    private Map<String, Object> toVO(LearningMaterial material) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", material.getId());
        map.put("activityId", material.getActivityId());
        map.put("title", material.getTitle());
        map.put("fileName", material.getFileName());
        map.put("fileSize", material.getFileSize());
        map.put("fileType", material.getFileType());
        map.put("description", material.getDescription());
        map.put("createdBy", material.getCreatedBy());
        map.put("createdAt", material.getCreatedAt());
        
        try {
            User creator = userService.getUserById(material.getCreatedBy());
            map.put("creatorName", creator.getRealName());
        } catch (Exception e) {
            map.put("creatorName", "");
        }
        
        try {
            if (material.getActivityId() != null) {
                Activity activity = activityService.getById(material.getActivityId());
                map.put("activityName", activity.getName());
            }
        } catch (Exception e) {
            map.put("activityName", "");
        }
        
        return map;
    }
}