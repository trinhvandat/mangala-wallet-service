package org.mangala.wallet.balance.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event published when wallet balances are updated.
 * Published to the 'balance.updates' topic.
 */
@Data
@Builder
public class BalanceUpdateEvent {

    /**
     * Unique event ID for idempotency.
     */
    private String eventId;

    /**
     * Event type identifier.
     */
    @Builder.Default
    private String eventType = "balance.updates";

    /**
     * The wallet ID whose balances were updated.
     */
    private UUID walletId;

    /**
     * The blockchain network (ethereum, bsc, polygon, arbitrum).
     */
    private String chain;

    /**
     * The wallet address.
     */
    private String address;

    /**
     * List of token balances.
     */
    private List<TokenBalancePayload> balances;

    /**
     * Timestamp when the sync occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    /**
     * Payload for individual token balance within the event.
     */
    @Data
    @Builder
    public static class TokenBalancePayload {
        private String symbol;
        private String contractAddress;  // null for native token
        private String amount;           // BigDecimal as string for precision
        private int decimals;
    }
}
