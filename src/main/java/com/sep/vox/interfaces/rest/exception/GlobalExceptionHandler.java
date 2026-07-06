package com.sep.vox.interfaces.rest.exception;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.interfaces.rest.dto.response.ErrorResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DUPLICATED_ERROR = "DUPLICATED";
    private static final String CONFLICT_ERROR = "CONFLICT";
    private static final String NOT_FOUND_ERROR = "NOT_FOUND";
    private static final String ILLEGAL_ARGUMENT_ERROR = "ILLEGAL_ARGUMENT";
    private static final String INVALID_STATE_ERROR = "INVALID_STATE";
    private static final String VALIDATION_ERROR = "BAD_REQUEST";
    private static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    private static final String FORBIDDEN_ERROR = "FORBIDDEN";
    private static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";

    private static final String AUTHENTICATION_ERROR = "BAD_CREDENTIALS";
    private static final String AUTHORIZATION_ERROR = "ACCESS_DENIED";
    private static final String USER_DISABLED_ERROR = "USER_DISABLED";
    private static final String VERSION_MISMATCHED_ERROR = "VERSION_MISMATCHED";

    @ExceptionHandler(DuplicatedException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicatedException e) {
        var error = new ErrorResponse(DUPLICATED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        var error = new ErrorResponse(CONFLICT_ERROR, e.getMessage());
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        LOGGER.error("An unexpected error occurred: {}", e.getMessage());
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


    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        var error = new ErrorResponse(VERSION_MISMATCHED_ERROR, "Dữ liệu đã được cập nhật bởi một yêu cầu khác. Vui lòng thử lại sau");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
