package org.mangala.wallet.portfolio.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_portfolio_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chain_type", "contract_address"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPortfolioBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chain_type", nullable = false, length = 20)
    private String chainType;

    @Column(name = "contract_address", length = 100)
    private String contractAddress;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "total_balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal totalBalance;

    @Column(name = "wallet_count", nullable = false)
    @Builder.Default
    private Integer walletCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;
}
