package com.sep.vox.interfaces.rest.exception;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.interfaces.rest.dto.response.ErrorResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationErrorResponse;

@RestControllerAdvice
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
    private static final String CONCURRENT_UPDATE_ERROR = "CONCURRENT_UPDATE";
    private static final String SERVICE_UNAVAILABLE_ERROR = "SERVICE_UNAVAILABLE";

    private static final String AUTHENTICATION_ERROR = "BAD_CREDENTIALS";
    private static final String AUTHORIZATION_ERROR = "ACCESS_DENIED";
    private static final String USER_DISABLED_ERROR = "USER_DISABLED";

    @ExceptionHandler(DuplicatedException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicatedException e) {
        var error = new ErrorResponse(DUPLICATED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        var error = new ErrorResponse(NOT_FOUND_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        var error = new ErrorResponse(ILLEGAL_ARGUMENT_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        var error = new ErrorResponse(INVALID_STATE_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        var error = new ErrorResponse(UNAUTHORIZED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        var errors = new HashMap<String, String>();
        e.getBindingResult()
            .getAllErrors()
            .forEach(err -> errors
                .put(err.getObjectName(), err.getDefaultMessage()));
        var error = new ValidationErrorResponse(errors, VALIDATION_ERROR);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }



    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        var error = new ErrorResponse(FORBIDDEN_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException e) {
        var error = new ErrorResponse(QUOTA_EXCEEDED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePlanLimitExceeded(PlanLimitExceededException e) {
        var error = new ErrorResponse(PLAN_LIMIT_EXCEEDED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    // Bản ghi (vd SchoolSubscription) đã bị nơi khác đổi (thao tác song song, hoặc
    // SubscriptionExpiryJob) giữa lúc đọc và lúc lưu — @Version phát hiện lệch, Hibernate ném
    // OptimisticLockException, Spring dịch sang exception này. Trả 409 để client biết cần tải
    // lại dữ liệu mới nhất rồi thử lại, thay vì âm thầm ghi đè hoặc crash 500.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException e) {
        var error = new ErrorResponse(CONCURRENT_UPDATE_ERROR, "Dữ liệu vừa được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(ServiceUnavailableException e) {
        var error = new ErrorResponse(SERVICE_UNAVAILABLE_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        LOGGER.error("An unexpected error occurred", e);
        var error = new ErrorResponse(INTERNAL_ERROR, "Có lỗi xảy ra");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Spring security exceptions
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        var error = new ErrorResponse(AUTHENTICATION_ERROR, "Sai thông tin đăng nhập");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        var error = new ErrorResponse(AUTHORIZATION_ERROR, "Quyền truy cập không hợp lệ");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(DisabledException.class)
    private ResponseEntity<ErrorResponse> handleDisabled(DisabledException e) {
        var error = new ErrorResponse(USER_DISABLED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
