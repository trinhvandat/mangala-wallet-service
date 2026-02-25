package org.mangala.wallet.balance.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes balance update events to Kafka.
 * Events are published to the 'balance.updates' topic after successful sync.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBalancePublisher {

    private static final String TOPIC = "balance.updates";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    /**
     * Publishes a balance update event for a wallet.
     *
     * @param wallet   The wallet entity
     * @param balances The updated balances
     */
    public void publishBalanceUpdate(WalletEntity wallet, List<WalletBalanceEntity> balances) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping balance update event for wallet {}", wallet.getId());
            return;
        }

        try {
            BalanceUpdateEvent event = buildEvent(wallet, balances);
            String eventJson = objectMapper.writeValueAsString(event);

            // Use wallet ID as key for partition consistency
            String key = wallet.getId().toString();

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish balance update for wallet {}: {}",
                            wallet.getId(), ex.getMessage());
                } else {
                    log.debug("Published balance update for wallet {} to partition {} offset {}",
                            wallet.getId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize balance update event for wallet {}: {}",
                    wallet.getId(), e.getMessage());
        }
    }

    /**
     * Publishes a balance update event synchronously (for testing or critical paths).
     *
     * @param wallet   The wallet entity
     * @param balances The updated balances
     * @return true if published successfully, false otherwise
     */
    public boolean publishBalanceUpdateSync(WalletEntity wallet, List<WalletBalanceEntity> balances) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping balance update event for wallet {}", wallet.getId());
            return true;
        }

        try {
            BalanceUpdateEvent event = buildEvent(wallet, balances);
            String eventJson = objectMapper.writeValueAsString(event);
            String key = wallet.getId().toString();

            SendResult<String, String> result = kafkaTemplate.send(TOPIC, key, eventJson).get();
            log.info("Published balance update for wallet {} to partition {} offset {}",
                    wallet.getId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;

        } catch (Exception e) {
            log.error("Failed to publish balance update for wallet {}: {}",
                    wallet.getId(), e.getMessage());
            return false;
        }
    }

    private BalanceUpdateEvent buildEvent(WalletEntity wallet, List<WalletBalanceEntity> balances) {
        List<BalanceUpdateEvent.TokenBalancePayload> payloads = balances.stream()
                .map(b -> BalanceUpdateEvent.TokenBalancePayload.builder()
                        .symbol(b.getSymbol())
                        .contractAddress(b.getContractAddress())
                        .amount(b.getBalance().toPlainString())
                        .decimals(b.getDecimals())
                        .build())
                .toList();

        return BalanceUpdateEvent.builder()
                .eventId(generateEventId(wallet.getId()))
                .walletId(wallet.getId())
                .chain(wallet.getChainType().toLowerCase())
                .address(wallet.getAddress())
                .balances(payloads)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Generates a unique event ID for idempotency.
     * Format: walletId-timestamp to ensure uniqueness.
     */
    private String generateEventId(UUID walletId) {
        return walletId.toString() + "-" + System.currentTimeMillis();
    }
}
