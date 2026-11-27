package com.sep.vox.interfaces.rest.exception;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.interfaces.rest.dto.response.ErrorResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationErrorResponse;

/**
 * Limited to {@code com.sep.vox}: without basePackages this advice covers EVERY controller in the context,
 * including those of libraries.
 *
 * <p>Specifically the Spring Boot Admin controllers ({@code de.codecentric...}). They serve long-running
 * {@code text/event-stream} SSE streams, and the browser closing the tab cuts them off midway --
 * which is perfectly normal. But {@code handleGeneric(Exception)} also catches that
 * {@code AsyncRequestNotUsableException}, logs it at ERROR as if it were a real incident, then tries to write a JSON
 * {@code ErrorResponse} into a response whose Content-Type is already fixed to {@code text/event-stream}
 * and dies a second time with {@code HttpMessageNotWritableException}.
 */
@RestControllerAdvice(basePackages = "com.sep.vox")
@Slf4j
public class GlobalExceptionHandler {

    private static final String DUPLICATED_ERROR = "DUPLICATED";
    private static final String NOT_FOUND_ERROR = "NOT_FOUND";
    private static final String ILLEGAL_ARGUMENT_ERROR = "ILLEGAL_ARGUMENT";
    private static final String VALIDATION_ERROR = "BAD_REQUEST";
    private static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    private static final String FORBIDDEN_ERROR = "FORBIDDEN";
    private static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String BUSINESS_RULE_VIOLATION_ERROR = "BUSINESS_RULE_VIOLATION";
    private static final String SERVICE_UNAVAILABLE_ERROR = "SERVICE_UNAVAILABLE";

    private static final String UNSUPPORTED_MEDIA_TYPE_ERROR = "UNSUPPORTED_MEDIA_TYPE";
    private static final String REQUEST_BODY_KEY = "body";
    private static final String ACCEPTED_VALUES_PREFIX = "Invalid value. Accepted values: ";

    private static final String AUTHENTICATION_ERROR = "BAD_CREDENTIALS";
    private static final String AUTHORIZATION_ERROR = "ACCESS_DENIED";
    private static final String USER_DISABLED_ERROR = "USER_DISABLED";

    @ExceptionHandler(ResourceDuplicatedException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(ResourceDuplicatedException e) {
        ErrorResponse error = new ErrorResponse(DUPLICATED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        ErrorResponse error = new ErrorResponse(NOT_FOUND_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse(ILLEGAL_ARGUMENT_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Safety net: {@code DateTimeParseException} extends {@code DateTimeException}, NOT
     * {@code IllegalArgumentException}, so if it is not caught separately it falls to the generic handler and the client
     * gets a 500 even though the error is entirely caused by the submitted data. Use cases should parse through
     * {@code DateMapper.toInstant} -- that function now throws {@code IllegalArgumentException} with a
     * descriptive message; this branch only covers the places that have not been converted yet.
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException e) {
        ErrorResponse error = new ErrorResponse(ILLEGAL_ARGUMENT_ERROR, "The time value is not in ISO-8601 format");
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
        log.warn("Unreadable request body: {}", e.getCause() == null ? e.getClass() : e.getCause().getClass());
        if (e.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            Map<String, String> errors = Map.of(jsonPathOf(mismatch), describeInvalidValue(mismatch));
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors, VALIDATION_ERROR));
        }
        ErrorResponse error = new ErrorResponse(VALIDATION_ERROR, "The request body is not valid JSON");
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
            return "The time value is not in ISO-8601 format";
        }
        return "The value does not match the required data type";
    }

    private static String acceptedConstants(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(o -> o.toString()).collect(Collectors.joining(", "));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> requiredType = e.getRequiredType();
        String message = requiredType != null && requiredType.isEnum()
            ? ACCEPTED_VALUES_PREFIX + acceptedConstants(requiredType)
            : "The value does not match the required format";
        ValidationErrorResponse error = new ValidationErrorResponse(Map.of(e.getName(), message), VALIDATION_ERROR);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        Map<String, String> errors = Map.of(e.getParameterName(), "Required parameter must not be blank");
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
        String message = "Unsupported Content-Type. Accepted types: "
            + supported.stream().map(mt -> mt.toString()).collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(UNSUPPORTED_MEDIA_TYPE_ERROR, message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }



    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        ErrorResponse error = new ErrorResponse(FORBIDDEN_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException e) {
        ErrorResponse error = new ErrorResponse(BUSINESS_RULE_VIOLATION_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(ServiceUnavailableException e) {
        ErrorResponse error = new ErrorResponse(SERVICE_UNAVAILABLE_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("An unexpected error occurred", e);
        ErrorResponse error = new ErrorResponse(INTERNAL_ERROR, "An error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Spring security exceptions
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        ErrorResponse error = new ErrorResponse(AUTHENTICATION_ERROR, "Incorrect login credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse error = new ErrorResponse(AUTHORIZATION_ERROR, "Access permission is invalid");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException e) {
        ErrorResponse error = new ErrorResponse(USER_DISABLED_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}