package com.douzone_internship.backend.exceptions;

import com.douzone_internship.backend.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
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
                path
        );
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

    // @Validated 를 통한 파라미터/경로변수 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
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

    // 그 외 모든 예외 - 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        String path = extractPath(request);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                path
        );
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
