package org.mangala.wallet.balance.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a token balance for a wallet.
 * Each record stores one token's balance (native or ERC-20) for a specific wallet.
 */
@Entity
@Table(name = "wallet_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WalletBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "chain_type", nullable = false, length = 20)
    private String chainType;

    @Column(name = "contract_address", length = 100)
    private String contractAddress;  // NULL for native token

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "decimals", nullable = false)
    private Integer decimals;

    @Column(name = "balance_raw", nullable = false, length = 78)
    private String balanceRaw;  // BigInteger as string

    @Column(name = "balance", nullable = false, precision = 38, scale = 18)
    private BigDecimal balance;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if this balance is for a native token (ETH, BNB, MATIC, etc.)
     */
    public boolean isNativeToken() {
        return contractAddress == null;
    }
}
