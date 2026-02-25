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

    // Chain errors (CHN-xxx)
    CHAIN_NOT_SUPPORTED("CHN-001", "Chain type not supported", HttpStatus.BAD_REQUEST),
    CHAIN_UNAVAILABLE("CHN-002", "Chain RPC is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    // Balance errors (BAL-xxx)
    BALANCE_FETCH_FAILED("BAL-001", "Failed to fetch balance from chain", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}
