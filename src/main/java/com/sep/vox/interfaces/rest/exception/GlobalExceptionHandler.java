package com.sep.vox.interfaces.rest.exception;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.exception.WalletDrawConfirmationRequiredException;
import com.sep.vox.interfaces.rest.dto.response.ErrorResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationErrorResponse;

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
public class GlobalExceptionHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DUPLICATED_ERROR = "DUPLICATED";
    private static final String NOT_FOUND_ERROR = "NOT_FOUND";
    private static final String ILLEGAL_ARGUMENT_ERROR = "ILLEGAL_ARGUMENT";
    private static final String INVALID_STATE_ERROR = "INVALID_STATE";
    private static final String VALIDATION_ERROR = "BAD_REQUEST";
    private static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    private static final String FORBIDDEN_ERROR = "FORBIDDEN";
    private static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String QUOTA_EXCEEDED_ERROR = "QUOTA_EXCEEDED";
    private static final String PLAN_LIMIT_EXCEEDED_ERROR = "PLAN_LIMIT_EXCEEDED";
    private static final String SERVICE_UNAVAILABLE_ERROR = "SERVICE_UNAVAILABLE";
    private static final String WALLET_DRAW_CONFIRMATION_REQUIRED_ERROR = "WALLET_DRAW_CONFIRMATION_REQUIRED";

    private static final String UNSUPPORTED_MEDIA_TYPE_ERROR = "UNSUPPORTED_MEDIA_TYPE";
    private static final String REQUEST_BODY_KEY = "body";
    private static final String ACCEPTED_VALUES_PREFIX = "Giá trị không hợp lệ. Các giá trị chấp nhận: ";

    private static final String AUTHENTICATION_ERROR = "BAD_CREDENTIALS";
    private static final String AUTHORIZATION_ERROR = "ACCESS_DENIED";
    private static final String USER_DISABLED_ERROR = "USER_DISABLED";

    @ExceptionHandler(DuplicatedException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicatedException e) {
        ErrorResponse error = new ErrorResponse(DUPLICATED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        ErrorResponse error = new ErrorResponse(NOT_FOUND_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse(ILLEGAL_ARGUMENT_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Lưới an toàn: {@code DateTimeParseException} kế thừa {@code DateTimeException}, KHÔNG phải
     * {@code IllegalArgumentException}, nên nếu không bắt riêng thì rơi xuống handler chung và client
     * nhận 500 dù lỗi hoàn toàn do dữ liệu gửi lên. Các use case nên parse qua
     * {@code DateMapper.toInstant} — hàm đó đã đổi sang {@code IllegalArgumentException} với message
     * tiếng Việt; nhánh này chỉ đỡ những chỗ chưa chuyển.
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException e) {
        ErrorResponse error = new ErrorResponse(ILLEGAL_ARGUMENT_ERROR, "Giá trị thời gian không đúng định dạng ISO-8601");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        ErrorResponse error = new ErrorResponse(INVALID_STATE_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        ErrorResponse error = new ErrorResponse(UNAUTHORIZED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<String, String>();
        e.getBindingResult()
            .getAllErrors()
            .forEach(err -> errors
                .putIfAbsent(fieldNameOf(err), err.getDefaultMessage()));
        ValidationErrorResponse error = new ValidationErrorResponse(errors, VALIDATION_ERROR);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private static String fieldNameOf(ObjectError error) {
        return error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    }

    /**
     * Covers broken JSON, an unknown enum constant, a value of the wrong type and an unparsable date. The
     * response never repeats the framework message, because it contains internal class names.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableBody(HttpMessageNotReadableException e) {
        LOGGER.warn("Unreadable request body: {}", e.getCause() == null ? e.getClass() : e.getCause().getClass());
        if (e.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            Map<String, String> errors = Map.of(jsonPathOf(mismatch), describeInvalidValue(mismatch));
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors, VALIDATION_ERROR));
        }
        ErrorResponse error = new ErrorResponse(VALIDATION_ERROR, "Nội dung yêu cầu không phải JSON hợp lệ");
        return ResponseEntity.badRequest().body(error);
    }

    private static String jsonPathOf(JacksonException exception) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : exception.getPath()) {
            if (reference.getPropertyName() != null) {
                path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
            } else {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? REQUEST_BODY_KEY : path.toString();
    }

    private static String describeInvalidValue(MismatchedInputException mismatch) {
        Class<?> targetType = mismatch.getTargetType();
        if (targetType != null && targetType.isEnum()) {
            return ACCEPTED_VALUES_PREFIX + acceptedConstants(targetType);
        }
        if (targetType != null && Temporal.class.isAssignableFrom(targetType)) {
            return "Giá trị thời gian không đúng định dạng ISO-8601";
        }
        return "Giá trị không đúng kiểu dữ liệu yêu cầu";
    }

    private static String acceptedConstants(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(o -> o.toString()).collect(Collectors.joining(", "));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> requiredType = e.getRequiredType();
        String message = requiredType != null && requiredType.isEnum()
            ? ACCEPTED_VALUES_PREFIX + acceptedConstants(requiredType)
            : "Giá trị không đúng định dạng yêu cầu";
        ValidationErrorResponse error = new ValidationErrorResponse(Map.of(e.getName(), message), VALIDATION_ERROR);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        Map<String, String> errors = Map.of(e.getParameterName(), "Tham số bắt buộc không được để trống");
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors, VALIDATION_ERROR));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        Map<String, String> errors = new HashMap<String, String>();
        e.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(
            resolvable -> errors.putIfAbsent(result.getMethodParameter().getParameterName(),
                resolvable.getDefaultMessage())));
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors, VALIDATION_ERROR));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        List<MediaType> supported = e.getSupportedMediaTypes();
        String message = "Content-Type không được hỗ trợ. Các loại chấp nhận: "
            + supported.stream().map(mt -> mt.toString()).collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(UNSUPPORTED_MEDIA_TYPE_ERROR, message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }



    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        ErrorResponse error = new ErrorResponse(FORBIDDEN_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException e) {
        ErrorResponse error = new ErrorResponse(QUOTA_EXCEEDED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePlanLimitExceeded(PlanLimitExceededException e) {
        ErrorResponse error = new ErrorResponse(PLAN_LIMIT_EXCEEDED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(WalletDrawConfirmationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleWalletDrawConfirmationRequired(
            WalletDrawConfirmationRequiredException e) {
        ErrorResponse error = new ErrorResponse(WALLET_DRAW_CONFIRMATION_REQUIRED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(ServiceUnavailableException e) {
        ErrorResponse error = new ErrorResponse(SERVICE_UNAVAILABLE_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        LOGGER.error("An unexpected error occurred", e);
        ErrorResponse error = new ErrorResponse(INTERNAL_ERROR, "Có lỗi xảy ra");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Spring security exceptions
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        ErrorResponse error = new ErrorResponse(AUTHENTICATION_ERROR, "Sai thông tin đăng nhập");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse error = new ErrorResponse(AUTHORIZATION_ERROR, "Quyền truy cập không hợp lệ");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException e) {
        ErrorResponse error = new ErrorResponse(USER_DISABLED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}