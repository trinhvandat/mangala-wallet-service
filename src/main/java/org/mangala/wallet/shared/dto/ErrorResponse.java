package org.mangala.wallet.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response DTO for all API errors.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response for all API errors")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private final int status;

    @Schema(description = "Application-specific error code", example = "WAL-001")
    private final String errorCode;

    @Schema(description = "Human-readable error message", example = "Wallet not found")
    private final String message;

    @Schema(description = "ISO-8601 timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    private final Instant timestamp;

    @Schema(description = "Request path that caused the error", example = "/api/v1/wallets")
    private final String path;

    @Schema(description = "Field-level validation errors (field name -> error message)")
    private final Map<String, String> errors;

    @Schema(description = "Trace ID for debugging (from X-Trace-Id header)", example = "abc123")
    private final String traceId;
}
