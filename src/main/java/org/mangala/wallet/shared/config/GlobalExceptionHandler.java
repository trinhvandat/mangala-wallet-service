package org.mangala.wallet.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.dto.ErrorResponse;
import org.mangala.wallet.shared.exception.WalletException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for standardized error responses.
 * All exceptions are caught here and transformed into consistent ErrorResponse format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * Handle application-specific WalletException
     */
    @ExceptionHandler(WalletException.class)
    public ResponseEntity<ErrorResponse> handleWalletException(
            WalletException ex, HttpServletRequest request) {

        log.warn("Wallet exception: {} - {} [path={}]",
                ex.getErrorDefinition().getErrorCode(),
                ex.getErrorDefinition().getErrorMessage(),
                request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                ex.getErrorDefinition().getHttpStatus().value(),
                ex.getErrorDefinition().getErrorCode(),
                ex.getErrorDefinition().getErrorMessage(),
                request,
                null);

        return ResponseEntity
                .status(ex.getErrorDefinition().getHttpStatus())
                .body(response);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {} [path={}]", fieldErrors, request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorConstant.VALIDATION_FAILED.getErrorCode(),
                ErrorConstant.VALIDATION_FAILED.getErrorMessage(),
                request,
                fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing parameter: {} [path={}]", ex.getParameterName(), request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorConstant.MISSING_REQUIRED_FIELD.getErrorCode(),
                "Missing required parameter: " + ex.getParameterName(),
                request,
                null);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle type mismatch (e.g., invalid UUID format)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        log.warn("Type mismatch: {} [path={}]", message, request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorConstant.VALIDATION_FAILED.getErrorCode(),
                message,
                request,
                null);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle malformed JSON in request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Invalid request body [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorConstant.INVALID_REQUEST_BODY.getErrorCode(),
                ErrorConstant.INVALID_REQUEST_BODY.getErrorMessage(),
                request,
                null);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle unsupported HTTP methods
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not supported: {} [path={}]", ex.getMethod(), request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "GEN-005",
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                request,
                null);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle unsupported media types
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Media type not supported: {} [path={}]", ex.getContentType(), request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "GEN-006",
                "Content type " + ex.getContentType() + " is not supported",
                request,
                null);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handle 404 - endpoint not found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("Endpoint not found: {} {} [path={}]",
                ex.getHttpMethod(), ex.getRequestURL(), request.getRequestURI());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ErrorConstant.RESOURCE_NOT_FOUND.getErrorCode(),
                "Endpoint not found: " + ex.getRequestURL(),
                request,
                null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle Spring Security authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorConstant.UNAUTHORIZED.getErrorCode(),
                ErrorConstant.UNAUTHORIZED.getErrorMessage(),
                request,
                null);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied [path={}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ErrorConstant.ACCESS_DENIED.getErrorCode(),
                ErrorConstant.ACCESS_DENIED.getErrorMessage(),
                request,
                null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // Log full stack trace for unexpected errors
        log.error("Unexpected error [path={}]", request.getRequestURI(), ex);

        ErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorConstant.INTERNAL_ERROR.getErrorCode(),
                ErrorConstant.INTERNAL_ERROR.getErrorMessage(),
                request,
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Build standardized error response
     */
    private ErrorResponse buildErrorResponse(
            int status,
            String errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .traceId(request.getHeader(TRACE_ID_HEADER))
                .errors(fieldErrors)
                .build();
    }
}
