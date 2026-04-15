package com.school.teacherEval.exception;

import com.school.teacherEval.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, 错误: {}", request.getRequestURI(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 - URI: {}, 错误: {}", request.getRequestURI(), e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败 - URI: {}, 错误: {}", request.getRequestURI(), errors);
        return ApiResponse.error(400, errors);
    }

    /**
     * 处理权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足 - URI: {}, 用户: {}", request.getRequestURI(), request.getUserPrincipal());
        return ApiResponse.error(403, "权限不足");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未处理异常 - URI: {}, 错误: {}", request.getRequestURI(), e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}