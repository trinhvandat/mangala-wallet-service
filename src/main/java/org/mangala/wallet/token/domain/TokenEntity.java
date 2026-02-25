package org.mangala.wallet.token.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "chain_type", nullable = false, length = 20)
    private String chainType;

    @Column(name = "contract_address", nullable = false, length = 100)
    private String contractAddress;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "decimals", nullable = false)
    private Integer decimals;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "coingecko_id", length = 100)
    private String coingeckoId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
