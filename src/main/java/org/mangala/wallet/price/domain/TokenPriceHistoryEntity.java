package org.mangala.wallet.price.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_price_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "chain_type", nullable = false, length = 20)
    private String chainType;

    @Column(name = "price_usd", nullable = false, precision = 30, scale = 18)
    private BigDecimal priceUsd;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
