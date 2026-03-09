package org.mangala.wallet.shared.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mangala.exception.ErrorDefinition;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorConstant implements ErrorDefinition {

    // Wallet errors (WAL-xxx)
    WALLET_NOT_FOUND("WAL-001", "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_ALREADY_EXISTS("WAL-002", "Wallet already exists for this address and chain", HttpStatus.CONFLICT),
    WALLET_ACCESS_DENIED("WAL-003", "Access denied to this wallet", HttpStatus.FORBIDDEN),
    INVALID_WALLET_ADDRESS("WAL-004", "Invalid wallet address format", HttpStatus.BAD_REQUEST),
    WALLET_INACTIVE("WAL-005", "Wallet is inactive", HttpStatus.BAD_REQUEST),

    // Chain errors (CHN-xxx)
    CHAIN_NOT_SUPPORTED("CHN-001", "Chain type not supported", HttpStatus.BAD_REQUEST),
    CHAIN_UNAVAILABLE("CHN-002", "Chain RPC is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    CHAIN_TIMEOUT("CHN-003", "Chain request timed out", HttpStatus.GATEWAY_TIMEOUT),

    // Balance errors (BAL-xxx)
    BALANCE_FETCH_FAILED("BAL-001", "Failed to fetch balance from chain", HttpStatus.INTERNAL_SERVER_ERROR),
    BALANCE_NOT_FOUND("BAL-002", "Balance not found", HttpStatus.NOT_FOUND),

    // Token errors (TKN-xxx)
    TOKEN_NOT_FOUND("TKN-001", "Token not found", HttpStatus.NOT_FOUND),
    TOKEN_NOT_SUPPORTED("TKN-002", "Token not supported on this chain", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN_ADDRESS("TKN-003", "Invalid token contract address", HttpStatus.BAD_REQUEST),

    // Transaction errors (TXN-xxx)
    TRANSACTION_NOT_FOUND("TXN-001", "Transaction not found", HttpStatus.NOT_FOUND),
    TRANSACTION_FETCH_FAILED("TXN-002", "Failed to fetch transaction from chain", HttpStatus.INTERNAL_SERVER_ERROR),

    // Validation errors (VAL-xxx)
    VALIDATION_FAILED("VAL-001", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY("VAL-002", "Invalid request body", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("VAL-003", "Missing required field", HttpStatus.BAD_REQUEST),

    // Authentication/Authorization errors (AUTH-xxx)
    UNAUTHORIZED("AUTH-001", "Unauthorized access", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH-002", "Access denied", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("AUTH-003", "Invalid authentication token", HttpStatus.UNAUTHORIZED),

    // General errors (GEN-xxx)
    INTERNAL_ERROR("GEN-001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("GEN-002", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    RATE_LIMIT_EXCEEDED("GEN-003", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    RESOURCE_NOT_FOUND("GEN-004", "Resource not found", HttpStatus.NOT_FOUND);

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}
