package org.mangala.wallet.portfolio.usecase;

import org.mangala.wallet.portfolio.adapter.web.dto.PortfolioSummaryResponse;

import java.util.UUID;

/**
 * Use case for retrieving a user's portfolio summary.
 */
public interface GetPortfolioSummaryUseCase {

    /**
     * Get the complete portfolio summary for a user.
     *
     * @param userId The user ID
     * @return Portfolio summary with total value, token breakdown, and chain summary
     */
    PortfolioSummaryResponse getPortfolioSummary(UUID userId);

    /**
     * Get portfolio summary filtered by chain.
     *
     * @param userId The user ID
     * @param chainType The chain type to filter by (e.g., "ETHEREUM", "BSC")
     * @return Filtered portfolio summary
     */
    PortfolioSummaryResponse getPortfolioSummaryByChain(UUID userId, String chainType);
}
