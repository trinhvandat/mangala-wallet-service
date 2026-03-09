package org.mangala.wallet.portfolio.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.event.BalanceUpdateEvent;
import org.mangala.wallet.portfolio.service.PortfolioAggregationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for balance update events.
 * Processes events from the balance.updates topic and aggregates portfolio data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BalanceUpdateConsumer {

    private static final String TOPIC = "balance.updates";

    private final ObjectMapper objectMapper;
    private final PortfolioAggregationService portfolioAggregationService;

    @KafkaListener(
            topics = TOPIC,
            groupId = "${spring.kafka.consumer.group-id:wallet-balance-aggregator}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        long startTime = System.currentTimeMillis();
        log.debug("Received balance update: key={}, partition={}, offset={}", key, partition, offset);

        try {
            BalanceUpdateEvent event = objectMapper.readValue(message, BalanceUpdateEvent.class);

            portfolioAggregationService.processBalanceUpdate(event);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("Processed balance update for wallet {} in {}ms", event.getWalletId(), processingTime);

            // Acknowledge after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process balance update: key={}, error={}", key, e.getMessage(), e);
            // Don't acknowledge - message will be redelivered
            // In production, consider dead-letter queue after max retries
        }
    }
}
