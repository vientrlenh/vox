package com.sep.vox.interfaces.rest.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationResponse;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.ValueInstantiationException;

/**
 * Giới hạn trong {@code com.sep.vox}: không khai basePackages thì advice này phủ lên MỌI
 * controller trong context, kể cả của thư viện.
 *
 * <p>Cụ thể là các controller của Spring Boot Admin ({@code de.codecentric...}). Chúng phục vụ
 * stream SSE {@code text/event-stream} chạy dài, và trình duyệt đóng tab là ngắt giữa chừng --
 * chuyện hoàn toàn bình thường. Nhưng {@code handleGeneric(Exception)} bắt luôn cả
 * {@code AsyncRequestNotUsableException} đó, ghi log ERROR như một sự cố thật, rồi cố ghi
 * {@code ErrorResponse} dạng JSON vào response đã chốt Content-Type là {@code text/event-stream}
 * và chết tiếp lần nữa với {@code HttpMessageNotWritableException}.
 */
@RestControllerAdvice(basePackages = "com.sep.vox")
@Slf4j 
public class RestApiGlobalExceptionHandler {

    @ExceptionHandler(ResourceDuplicatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(ResourceDuplicatedException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * The default of "Bad request exception" in business code
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(ServiceUnavailableException ex) {
        ApiResponse<Void> error = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("An unexpected error occurred {}", ex.getMessage(), ex);
        ApiResponse<Void> error = ApiResponse.error("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

// ================================ SPRING VALIDATION EXCEPTIONS =======================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
            .getAllErrors()
            .forEach((err) -> {
                String fieldName = ((FieldError) err).getField();
                String msg = err.getDefaultMessage();
                errors.put(fieldName, msg);
            });
        return validationErr(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        Map<String, String> errors = new HashMap<>();
        if (cause instanceof InvalidFormatException ife) {
            errors.put(path(ife), describe(ife));
            return validationErr(errors);
        }
        if (cause instanceof MismatchedInputException mie) {
            errors.put(path(mie), "Wrong type or shape for this field");
            return validationErr(errors);
        }
        if (cause instanceof ValueInstantiationException vie) {
            errors.put(path(vie), vie.getCause().getMessage());
            return validationErr(errors);
        }
        errors.put("Malformed JSON input", null);
        return validationErr(errors);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationResponse> handleParamMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '%s'".formatted(ex.getValue());
        Class<?> type = ex.getRequiredType();
        if (type != null && type.isEnum()) {
            message += ". Allowed: " + Arrays.toString(type.getEnumConstants());
        }
        Map<String, String> errors = Map.of(ex.getName(), message);
        return validationErr(errors);
    }

    // =============================================== SPRING SECURITY EXCEPTION ================================================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        ApiResponse<Void> error = ApiResponse.error("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        ApiResponse<Void> error = ApiResponse.error("You are not allowed to perform this action");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException e) {
        ApiResponse<Void> error = ApiResponse.error("User is disabled");
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }


    private ResponseEntity<ValidationResponse> validationErr(Map<String, String> errors) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationResponse.error(errors));
    }

    private String describe(InvalidFormatException ex) {
        Class<?> type = ex.getTargetType();
        Object value = ex.getValue();

        if (type.isEnum()) {
            return "Invalid value '%s'. Allowed: %s".formatted(value, Arrays.toString(type.getEnumConstants()));
        }
        if (type == LocalDate.class) {
            return "Invalid date '%s'. Expected format: yyyy-MM-dd".formatted(value);
        }
        if (type == Instant.class) {
            return "Invalid timestamp '%s'. Expected ISO-8601 UTC, e.g. 2000-01-30T10:00:00Z".formatted(value);
        }
        return "Invalid value '%s'. Expected type: '%s'".formatted(value, type.getSimpleName());
    } 

    private String path(JacksonException ex) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getPropertyName());
            } else {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }
}