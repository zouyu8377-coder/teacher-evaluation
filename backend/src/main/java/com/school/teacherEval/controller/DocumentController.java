package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.DocumentService;
import com.school.teacherEval.service.UserService;
import com.school.teacherEval.vo.DocumentVO;
import com.school.teacherEval.vo.PageVO;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理")
public class DocumentController {
    
    private final DocumentService documentService;
    private final UserService userService;
    
    @GetMapping
    @Operation(summary = "获取文档列表")
    public ApiResponse<PageVO<DocumentVO>> getDocuments(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);

        if (currentUser.getRole() == User.Role.teacher) {
            userId = currentUser.getId();
        }

        Page<Document> docPage = documentService.getDocuments(userId, activityId, page, size);
        List<DocumentVO> records = docPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        PageVO<DocumentVO> data = new PageVO<>(records, docPage.getTotalElements(), page, size);

        return ApiResponse.success(data);
    }
    
    @PostMapping
    @Operation(summary = "上传文档")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("activityId") Long activityId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) throws Exception {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        Document document = documentService.uploadDocument(
                file, currentUser.getId(), activityId, title, description);
        
        return ApiResponse.success(document);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情")
    public ApiResponse<DocumentVO> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id);
        return ApiResponse.success(toVO(document));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新文档信息")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<Document> updateDocument(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        Document document = documentService.updateDocument(id, currentUser.getId(), title, description);
        return ApiResponse.success(document);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);
        
        documentService.deleteDocument(id, currentUser.getId(), currentUser.getRole().name());
        return ApiResponse.success("删除成功", null);
    }
    
    @GetMapping("/{id}/download")
    @Operation(summary = "下载文档")
    public void downloadDocument(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Document document = documentService.getDocumentById(id);
        String fileName = document.getFileName();
        
        response.setContentType(document.getFileType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        
        InputStream inputStream = documentService.downloadDocument(id);
        OutputStream outputStream = response.getOutputStream();
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        
        inputStream.close();
        outputStream.flush();
    }
    
    private DocumentVO toVO(Document doc) {
        String realName = "";
        try {
            User teacher = userService.getUserById(doc.getUserId());
            realName = teacher.getRealName();
        } catch (Exception e) {
            // ignore
        }
        return new DocumentVO(
                doc.getId(),
                doc.getUserId(),
                doc.getActivityId(),
                doc.getTitle(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getFileType(),
                doc.getDescription(),
                doc.getCreatedAt(),
                realName
        );
    }
}