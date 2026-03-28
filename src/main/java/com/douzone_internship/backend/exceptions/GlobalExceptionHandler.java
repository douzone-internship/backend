package com.douzone_internship.backend.exceptions;

import com.douzone_internship.backend.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 필수 요청 파라미터 누락 처리 (?name= 등)
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.format("Required request parameter '%s' is missing", ex.getParameterName()),
                path);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // @Valid 본문/DTO 검증 실패
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String path = extractPath(request);
        BindingResult bindingResult = ex.getBindingResult();
        List<ErrorResponse.FieldErrorDetail> details = bindingResult.getFieldErrors().stream()
                .map(this::toDetail)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(path)
                .fieldErrors(details)
                .build();
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // JSON 파싱 실패 (잘못된 요청 본문)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "요청 본문을 파싱할 수 없습니다. JSON 형식을 확인해주세요.",
                path);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // @Validated 를 통한 파라미터/경로변수 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        String path = extractPath(request);
        List<ErrorResponse.FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(this::toDetail)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Constraint violation")
                .path(path)
                .fieldErrors(details)
                .build();
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 비즈니스 예외: 리소스 없음 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                path);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // 비즈니스 예외: 권한 없음 (403)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                path);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // 비즈니스 예외: 중복 리소스 (409)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                path);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // 경로변수/파라미터 타입 불일치 (예: UUID 자리에 잘못된 값)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String path = extractPath(request);
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("파라미터 '%s'의 값이 올바르지 않습니다. (기대 타입: %s)", paramName, requiredType);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Spring Security 접근 거부 (인증은 됐지만 권한 없음)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "접근 권한이 없습니다.",
                path);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // 비즈니스 예외: 잘못된 요청 (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                path);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 그 외 모든 예외 - 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                path);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "Server error";
    }

    private ErrorResponse.FieldErrorDetail toDetail(FieldError fe) {
        return ErrorResponse.FieldErrorDetail.builder()
                .field(fe.getField())
                .rejectedValue(fe.getRejectedValue() == null ? null : fe.getRejectedValue().toString())
                .reason(fe.getDefaultMessage())
                .build();
    }

    private ErrorResponse.FieldErrorDetail toDetail(ConstraintViolation<?> cv) {
        String field = cv.getPropertyPath() == null ? null : cv.getPropertyPath().toString();
        String rejected = cv.getInvalidValue() == null ? null : cv.getInvalidValue().toString();
        return ErrorResponse.FieldErrorDetail.builder()
                .field(field)
                .rejectedValue(rejected)
                .reason(cv.getMessage())
                .build();
    }
}
