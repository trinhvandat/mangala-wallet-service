package org.mangala.wallet.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.shared.exception.WalletException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletException.class)
    public ResponseEntity<Map<String, Object>> handleWalletException(WalletException ex) {
        log.warn("Wallet exception: {} - {}",
                ex.getErrorDefinition().getErrorCode(),
                ex.getErrorDefinition().getErrorMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getErrorDefinition().getErrorCode());
        body.put("message", ex.getErrorDefinition().getErrorMessage());

        return ResponseEntity
                .status(ex.getErrorDefinition().getHttpStatus())
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "VAL-001");
        body.put("message", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }
}
