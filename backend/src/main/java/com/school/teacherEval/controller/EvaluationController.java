package com.school.teacherEval.controller;

import com.school.teacherEval.dto.ApiResponse;
import com.school.teacherEval.entity.Evaluation;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.service.EvaluationService;
import com.school.teacherEval.service.UserService;
import com.school.teacherEval.vo.EvaluationListVO;
import com.school.teacherEval.vo.EvaluationSummaryVO;
import com.school.teacherEval.vo.EvaluationVO;
import com.school.teacherEval.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "考核评分管理")
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);

    private final EvaluationService evaluationService;
    private final UserService userService;
    
    @GetMapping
    @Operation(summary = "获取评分列表")
    public ApiResponse<PageVO<EvaluationVO>> getEvaluations(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);

        if (currentUser.getRole() == User.Role.teacher) {
            teacherId = currentUser.getId();
        }

        Page<Evaluation> evalPage = evaluationService.getEvaluations(activityId, teacherId, page, size);
        List<EvaluationVO> records = evalPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        PageVO<EvaluationVO> data = new PageVO<>(records, evalPage.getTotalElements(), page, size);

        return ApiResponse.success(data);
    }

    @GetMapping("/teacher/me")
    @Operation(summary = "教师查看自己的成绩")
    @PreAuthorize("hasRole('teacher')")
    public ApiResponse<List<EvaluationVO>> getMyEvaluations() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);

        List<Evaluation> evaluations = evaluationService.getTeacherPublishedEvaluations(currentUser.getId());

        List<EvaluationVO> result = evaluations.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @GetMapping("/activity/{activityId}/teacher/{teacherId}")
    @Operation(summary = "获取某教师的所有评分")
    public ApiResponse<EvaluationListVO> getTeacherActivityEvaluations(
            @PathVariable Long activityId,
            @PathVariable Long teacherId) {

        List<Evaluation> evaluations = evaluationService.getActivityTeacherEvaluations(activityId, teacherId);
        BigDecimal avgScore = evaluationService.calculateAverageScore(evaluations);

        EvaluationListVO data = new EvaluationListVO(
                evaluations.stream().map(this::toVO).collect(Collectors.toList()),
                evaluations.size(),
                avgScore
        );

        return ApiResponse.success(data);
    }

    @GetMapping("/activity/{activityId}/summary")
    @Operation(summary = "获取活动评分汇总")
    public ApiResponse<EvaluationSummaryVO> getActivitySummary(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long teacherId) {

        EvaluationService.EvaluationSummary summary = evaluationService.getActivitySummary(activityId, teacherId);

        EvaluationSummaryVO data = new EvaluationSummaryVO(
                summary.getTotalEvaluations(),
                summary.getAverageScore(),
                summary.getEvaluations() != null
                    ? summary.getEvaluations().stream().map(this::toVO).collect(Collectors.toList())
                    : List.of()
        );

        return ApiResponse.success(data);
    }
    
    @PostMapping
    @Operation(summary = "提交评分")
    @PreAuthorize("hasRole('evaluator') or hasRole('admin')")
    public ApiResponse<Evaluation> createEvaluation(@RequestBody Map<String, Object> request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.getCurrentUser(username);

        log.info("提交评分请求: evaluator={}, teacherId={}, activityId={}, score={}",
                currentUser.getId(), request.get("teacherId"), request.get("activityId"), request.get("score"));

        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            Long activityId = Long.valueOf(request.get("activityId").toString());
            BigDecimal score = new BigDecimal(request.get("score").toString());
            String comment = request.get("comment") != null ? request.get("comment").toString() : null;

            Evaluation evaluation = evaluationService.createOrUpdateEvaluation(
                    currentUser.getId(), teacherId, activityId, score, comment);

            return ApiResponse.success(evaluation);
        } catch (Exception e) {
            log.error("提交评分失败", e);
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取评分详情")
    public ApiResponse<EvaluationVO> getEvaluation(@PathVariable Long id) {
        Evaluation evaluation = evaluationService.getEvaluationById(id);
        return ApiResponse.success(toVO(evaluation));
    }

    @PostMapping("/publish")
    @Operation(summary = "公布成绩")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Integer> publishScores(
            @RequestParam Long activityId,
            @RequestParam(required = false) Long teacherId) {
        int count = evaluationService.publishScores(activityId, teacherId);
        return ApiResponse.success(count);
    }

    private EvaluationVO toVO(Evaluation eval) {
        String evaluatorName = "";
        try {
            User evaluator = userService.getUserById(eval.getEvaluatorId());
            evaluatorName = evaluator.getRealName();
        } catch (Exception e) {
            // ignore
        }

        String teacherName = "";
        try {
            User teacher = userService.getUserById(eval.getTeacherId());
            teacherName = teacher.getRealName();
        } catch (Exception e) {
            // ignore
        }

        return new EvaluationVO(
                eval.getId(),
                eval.getActivityId(),
                eval.getEvaluatorId(),
                eval.getTeacherId(),
                eval.getScore(),
                eval.getFinalScore(),
                eval.getComment(),
                eval.getStatus() != null ? eval.getStatus().name() : null,
                eval.getIsPublished(),
                eval.getIsLocked(),
                eval.getCreatedAt(),
                evaluatorName,
                teacherName
        );
    }
}