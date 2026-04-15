package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.*;
import com.school.teacherEval.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
@Tag(name = "考试管理")
public class ExamController {
    
    private final ExamQuestionService questionService;
    private final ExamPaperService paperService;
    private final ExamRecordService recordService;
    private final UserService userService;
    
    // ========== 题库管理 ==========
    
    @GetMapping("/questions")
    @Operation(summary = "获取题库列表")
    public ApiResponse<Page<ExamQuestion>> getQuestions(
            @RequestParam(required = false) ExamQuestion.QuestionType type,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(questionService.getQuestions(type, status, page, size));
    }
    
    @GetMapping("/questions/{id}")
    @Operation(summary = "获取题目详情")
    public ApiResponse<ExamQuestion> getQuestion(@PathVariable Long id) {
        return ApiResponse.success(questionService.getById(id));
    }
    
    @PostMapping("/questions")
    @Operation(summary = "添加题目")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ExamQuestion> createQuestion(@RequestBody ExamQuestion question) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        question.setCreatedBy(user.getId());
        return ApiResponse.success(questionService.create(question));
    }
    
    @PutMapping("/questions/{id}")
    @Operation(summary = "修改题目")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ExamQuestion> updateQuestion(@PathVariable Long id, @RequestBody ExamQuestion question) {
        return ApiResponse.success(questionService.update(id, question));
    }
    
    @DeleteMapping("/questions/{id}")
    @Operation(summary = "删除题目")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.success(null);
    }
    
    @PostMapping("/questions/import")
    @Operation(summary = "Excel批量导入题目")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Integer> importQuestions(@RequestParam MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        int count = questionService.importFromExcel(file, user.getId());
        return ApiResponse.success(count);
    }
    
    @GetMapping("/questions/template")
    @Operation(summary = "下载题目导入模板")
    @PreAuthorize("hasRole('admin')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        Sheet singleSheet = workbook.createSheet("单选题");
        String[] singleHeaders = {"题目内容", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "正确答案", "分值", "解析"};
        Row singleHeaderRow = singleSheet.createRow(0);
        for (int i = 0; i < singleHeaders.length; i++) {
            singleHeaderRow.createCell(i).setCellValue(singleHeaders[i]);
        }
        Row singleExampleRow = singleSheet.createRow(1);
        singleExampleRow.createCell(0).setCellValue("教学设计的核心是以下哪个？");
        singleExampleRow.createCell(1).setCellValue("教材内容");
        singleExampleRow.createCell(2).setCellValue("教师讲授");
        singleExampleRow.createCell(3).setCellValue("学生学习");
        singleExampleRow.createCell(4).setCellValue("教学环境");
        singleExampleRow.createCell(5).setCellValue("");
        singleExampleRow.createCell(6).setCellValue("");
        singleExampleRow.createCell(7).setCellValue("C");
        singleExampleRow.createCell(8).setCellValue("5");
        singleExampleRow.createCell(9).setCellValue("教学设计应围绕学生的学习需求展开");
        
        Sheet multiSheet = workbook.createSheet("多选题");
        String[] multiHeaders = {"题目内容", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "正确答案", "分值", "解析"};
        Row multiHeaderRow = multiSheet.createRow(0);
        for (int i = 0; i < multiHeaders.length; i++) {
            multiHeaderRow.createCell(i).setCellValue(multiHeaders[i]);
        }
        Row multiExampleRow = multiSheet.createRow(1);
        multiExampleRow.createCell(0).setCellValue("以下哪些是有效的教学策略？");
        multiExampleRow.createCell(1).setCellValue("讲授法");
        multiExampleRow.createCell(2).setCellValue("讨论法");
        multiExampleRow.createCell(3).setCellValue("演示法");
        multiExampleRow.createCell(4).setCellValue("练习法");
        multiExampleRow.createCell(5).setCellValue("反馈法");
        multiExampleRow.createCell(6).setCellValue("评估法");
        multiExampleRow.createCell(7).setCellValue("ABCDEF");
        multiExampleRow.createCell(8).setCellValue("5");
        multiExampleRow.createCell(9).setCellValue("以上都是常用的教学策略");
        
        for (int i = 0; i < 10; i++) {
            singleSheet.setColumnWidth(i, 4000);
            multiSheet.setColumnWidth(i, 4000);
        }
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=C级题库导入模板.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    // ========== 试卷管理 ==========
    
    @GetMapping("/papers")
    @Operation(summary = "获取试卷列表")
    public ApiResponse<Page<ExamPaper>> getPapers(
            @RequestParam(required = false) Long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(paperService.getPapers(activityId, page, size));
    }
    
    @GetMapping("/papers/activity/{activityId}")
    @Operation(summary = "获取活动的试卷列表")
    public ApiResponse<List<ExamPaper>> getPapersByActivity(@PathVariable Long activityId) {
        return ApiResponse.success(paperService.getPapersByActivity(activityId));
    }
    
    @GetMapping("/papers/{id}")
    @Operation(summary = "获取试卷详情")
    public ApiResponse<ExamPaper> getPaper(@PathVariable Long id) {
        return ApiResponse.success(paperService.getById(id));
    }
    
    @GetMapping("/papers/{id}/questions")
    @Operation(summary = "获取试卷题目")
    public ApiResponse<List<PaperQuestion>> getPaperQuestions(@PathVariable Long id) {
        return ApiResponse.success(paperService.getPaperQuestions(id));
    }
    
    @PostMapping("/papers")
    @Operation(summary = "创建试卷")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ExamPaper> createPaper(@RequestBody ExamPaper paper) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        paper.setCreatedBy(user.getId());
        return ApiResponse.success(paperService.create(paper));
    }
    
    @PutMapping("/papers/{id}")
    @Operation(summary = "修改试卷")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ExamPaper> updatePaper(@PathVariable Long id, @RequestBody ExamPaper paper) {
        return ApiResponse.success(paperService.update(id, paper));
    }
    
    @DeleteMapping("/papers/{id}")
    @Operation(summary = "删除试卷")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> deletePaper(@PathVariable Long id) {
        paperService.delete(id);
        return ApiResponse.success(null);
    }
    
    @PutMapping("/papers/{id}/questions")
    @Operation(summary = "设置试卷题目")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> setPaperQuestions(@PathVariable Long id, @RequestBody List<Long> questionIds) {
        paperService.setPaperQuestions(id, questionIds);
        return ApiResponse.success(null);
    }
    
    @PostMapping("/papers/{id}/generate")
    @Operation(summary = "自动组卷")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ExamPaper> generatePaper(
            @PathVariable Long id,
            @RequestParam Integer singleCount,
            @RequestParam Integer multiCount) {
        return ApiResponse.success(paperService.generateRandomPaper(id, singleCount, multiCount));
    }
    
    @PostMapping("/papers/{paperId}/bind/{activityId}")
    @Operation(summary = "绑定试卷到活动")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> bindPaperToActivity(@PathVariable Long paperId, @PathVariable Long activityId) {
        paperService.bindToActivity(paperId, activityId);
        return ApiResponse.success(null);
    }
    
    // ========== 考试记录 ==========
    
    @PostMapping("/records/start")
    @Operation(summary = "开始考试")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<ExamRecord> startExam(@RequestParam Long activityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        return ApiResponse.success(recordService.startExam(activityId, user.getId()));
    }
    
    @GetMapping("/records/{id}")
    @Operation(summary = "获取考试题目")
    @PreAuthorize("hasRole('teacher') or hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Map<String, Object>> getExamQuestions(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        
        if (user.getRole() == User.Role.teacher) {
            return ApiResponse.success(recordService.getExamQuestions(id, user.getId()));
        } else {
            ExamRecord record = recordService.getRecordById(id);
            return ApiResponse.success(recordService.getExamQuestions(id, record.getTeacherId()));
        }
    }
    
    @PutMapping("/records/{id}/answer")
    @Operation(summary = "保存答案")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<ExamRecord> saveAnswer(@PathVariable Long id, @RequestBody Map<String, String> answers) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        return ApiResponse.success(recordService.saveAnswer(id, answers, user.getId()));
    }
    
    @PostMapping("/records/{id}/submit")
    @Operation(summary = "提交试卷")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<ExamRecord> submitExam(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        return ApiResponse.success(recordService.submitExam(id, user.getId()));
    }
    
    @GetMapping("/records/my")
    @Operation(summary = "我的考试记录")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<List<ExamRecord>> getMyRecords() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        return ApiResponse.success(recordService.getMyRecords(user.getId()));
    }
    
    @GetMapping("/records/activity/{activityId}")
    @Operation(summary = "获取活动的考试记录")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Page<ExamRecord>> getRecordsByActivity(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(recordService.getRecordsByActivity(activityId, page, size));
    }
    
    @GetMapping("/records/{id}/detail")
    @Operation(summary = "获取考试详情(含答案)")
    @PreAuthorize("hasRole('teacher') or hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Map<String, Object>> getExamDetail(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        // 教师只能查看自己的考试详情
        if (user.getRole() == User.Role.teacher) {
            ExamRecord record = recordService.getRecordById(id);
            if (!record.getTeacherId().equals(user.getId())) {
                return ApiResponse.error(403, "权限不足");
            }
        }
        return ApiResponse.success(recordService.getExamDetail(id));
    }
    
    @PostMapping("/records/{id}/adjust")
    @Operation(summary = "人工调整分数")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<ExamRecord> adjustScore(
            @PathVariable Long id,
            @RequestParam java.math.BigDecimal adjust) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getCurrentUser(auth.getName());
        return ApiResponse.success(recordService.adjustScore(id, adjust, user.getId()));
    }
}