package org.mangala.wallet.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.dto.ErrorResponse;
import org.mangala.wallet.shared.exception.WalletException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/wallets");
        when(mockRequest.getHeader("X-Trace-Id")).thenReturn("trace-123");
    }

    @Nested
    @DisplayName("handleWalletException")
    class HandleWalletException {

        @Test
        @DisplayName("should return standardized error response with all fields")
        void shouldReturnStandardizedErrorResponse() {
            WalletException ex = new WalletException(ErrorConstant.WALLET_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(404);
            assertThat(body.getErrorCode()).isEqualTo("WAL-001");
            assertThat(body.getMessage()).isEqualTo("Wallet not found");
            assertThat(body.getPath()).isEqualTo("/api/v1/wallets");
            assertThat(body.getTimestamp()).isNotNull();
            assertThat(body.getTraceId()).isEqualTo("trace-123");
        }

        @Test
        @DisplayName("should handle different error types")
        void shouldHandleDifferentErrorTypes() {
            WalletException ex = new WalletException(ErrorConstant.INVALID_WALLET_ADDRESS);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getErrorCode()).isEqualTo("WAL-004");
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationException {

        @Test
        @DisplayName("should include field-level errors")
        void shouldIncludeFieldErrors() throws NoSuchMethodException {
            Object target = new Object();
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
            bindingResult.addError(new FieldError("target", "address", "must not be blank"));
            bindingResult.addError(new FieldError("target", "chainType", "must not be null"));

            Method method = this.getClass().getDeclaredMethod("shouldIncludeFieldErrors");
            MethodParameter parameter = new MethodParameter(method, -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

            ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getErrorCode()).isEqualTo("VAL-001");
            assertThat(body.getErrors()).containsEntry("address", "must not be blank");
            assertThat(body.getErrors()).containsEntry("chainType", "must not be null");
        }
    }

    @Nested
    @DisplayName("handleMissingParams")
    class HandleMissingParams {

        @Test
        @DisplayName("should return error with parameter name")
        void shouldReturnErrorWithParameterName() {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("walletId", "UUID");

            ResponseEntity<ErrorResponse> response = handler.handleMissingParams(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).contains("walletId");
            assertThat(response.getBody().getErrorCode()).isEqualTo("VAL-003");
        }
    }

    @Nested
    @DisplayName("handleTypeMismatch")
    class HandleTypeMismatch {

        @Test
        @DisplayName("should return error with value and parameter name")
        void shouldReturnErrorWithDetails() {
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "not-a-uuid", String.class, "walletId", null, new IllegalArgumentException());

            ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).contains("not-a-uuid");
            assertThat(response.getBody().getMessage()).contains("walletId");
        }
    }

    @Nested
    @DisplayName("handleMessageNotReadable")
    class HandleMessageNotReadable {

        @Test
        @DisplayName("should return invalid request body error")
        void shouldReturnInvalidRequestBodyError() {
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "Malformed JSON", new MockHttpInputMessage(new byte[0]));

            ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getErrorCode()).isEqualTo("VAL-002");
        }
    }

    @Nested
    @DisplayName("handleMethodNotSupported")
    class HandleMethodNotSupported {

        @Test
        @DisplayName("should return 405 with method name")
        void shouldReturn405() {
            HttpRequestMethodNotSupportedException ex =
                    new HttpRequestMethodNotSupportedException("PATCH");

            ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody().getMessage()).contains("PATCH");
        }
    }

    @Nested
    @DisplayName("handleMediaTypeNotSupported")
    class HandleMediaTypeNotSupported {

        @Test
        @DisplayName("should return 415 unsupported media type")
        void shouldReturn415() {
            HttpMediaTypeNotSupportedException ex =
                    new HttpMediaTypeNotSupportedException("Unsupported");

            ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    @Nested
    @DisplayName("handleAuthenticationException")
    class HandleAuthenticationException {

        @Test
        @DisplayName("should return 401 unauthorized")
        void shouldReturn401() {
            BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

            ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
        }
    }

    @Nested
    @DisplayName("handleAccessDeniedException")
    class HandleAccessDeniedException {

        @Test
        @DisplayName("should return 403 forbidden")
        void shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-002");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return 500 for unexpected errors without exposing details")
        void shouldReturn500WithoutDetails() {
            RuntimeException ex = new RuntimeException("Database connection failed");

            ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getErrorCode()).isEqualTo("GEN-001");
            assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
            // Should NOT contain sensitive error details
            assertThat(response.getBody().getMessage()).doesNotContain("Database");
        }
    }

    @Nested
    @DisplayName("error response structure")
    class ErrorResponseStructure {

        @Test
        @DisplayName("should include timestamp")
        void shouldIncludeTimestamp() {
            WalletException ex = new WalletException(ErrorConstant.WALLET_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should include path")
        void shouldIncludePath() {
            WalletException ex = new WalletException(ErrorConstant.WALLET_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getBody().getPath()).isEqualTo("/api/v1/wallets");
        }

        @Test
        @DisplayName("should include trace ID from header")
        void shouldIncludeTraceId() {
            WalletException ex = new WalletException(ErrorConstant.WALLET_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getBody().getTraceId()).isEqualTo("trace-123");
        }

        @Test
        @DisplayName("should handle missing trace ID")
        void shouldHandleMissingTraceId() {
            when(mockRequest.getHeader("X-Trace-Id")).thenReturn(null);
            WalletException ex = new WalletException(ErrorConstant.WALLET_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleWalletException(ex, mockRequest);

            assertThat(response.getBody().getTraceId()).isNull();
        }
    }
}
