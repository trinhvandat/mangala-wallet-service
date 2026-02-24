# Wallet Service v1 - Microservice Design Analysis

## 1. Analysis Summary

Wallet Service v1 is the write-domain owner for user wallets and addresses.  
This service must provide a stable API contract, strict ownership validation, and integration-safe events for portfolio and other downstream services.

Business impact:
1. Decouples account source-of-truth from portfolio analytics.
2. Reduces policy/route drift by locking path + permission contract.
3. Enables multi-network onboarding with deterministic validation outcomes.

Success signals:
1. Gateway/auth/wallet path and permission mapping are aligned.
2. Portfolio can attach account only after wallet validation passes.
3. Duplicate account creation and cross-network mismatch are consistently blocked.

## 2. Scope And Acceptance Criteria

### In Scope
1. Wallet CRUD (`GET/POST/PUT/DELETE`) with soft-delete.
2. Address create/read APIs.
3. Internal account validation API for portfolio attach flow.
4. Multi-network validation rules (ETH/BSC/SOL for MVP).
5. Outbox-based event publishing for wallet/address lifecycle.
6. Idempotency handling for create operations.

### Out Of Scope
1. Portfolio valuation and P&L.
2. On-chain signature challenge ownership proof (v2).
3. Transfer execution/orchestration.

### Acceptance Criteria
1. Contract includes all v1 wallet/address endpoints and internal validate endpoint.
2. Ownership rule is enforced in all read/write operations.
3. Soft-delete is idempotent and auditable.
4. Validation API returns deterministic reason codes.
5. Event publishing is transactional (write + outbox in one DB transaction).

## 3. Domain Boundary

### Wallet Service Owns
1. Wallet lifecycle (create/update/archive).
2. Address/account lifecycle under a wallet.
3. Validation of wallet/account attachability for portfolio.
4. Source-of-truth for `(user, network, address)` ownership.

### Wallet Service Does Not Own
1. Portfolio composition and performance.
2. Market price, P&L, and holding valuation.
3. Notification rendering or message templates.

### Upstream/Downstream Contracts
1. Upstream:
   - Gateway (routing/auth context propagation).
   - Auth service (policy + JWT identity).
2. Downstream:
   - Portfolio service consumes wallet events and calls internal validation API.
   - History/notification consume wallet lifecycle events.

## 4. Database Strategy

Primary strategy: **single writer transactional store + outbox + read-model fanout**.

1. Transactional DB (PostgreSQL):
   - Tables: `wallets`, `addresses`, `idempotency_records`, `outbox_events`.
   - ACID transactions for create/update/archive and outbox insert.
2. Outbox pattern:
   - Persist domain event in `outbox_events` in same transaction as business write.
   - Relay publishes to Kafka asynchronously and marks published timestamp.
3. Idempotency:
   - Persist idempotency key + request hash + response snapshot with TTL.
   - Guarantees safe retry for create operations.
4. Query/index strategy:
   - Indexes for `(user_id, status, network)`, `(wallet_id, status)`, `(user_id, network, address)`.
   - Partial unique index for active addresses to avoid duplicate account attach source.
5. Data ownership rule:
   - No shared table/database with portfolio service.
   - Cross-service reads via API/event only.

## 5. User Stories

1. As a user, I create a wallet for a chosen network.
2. As a user, I add an account/address under my wallet.
3. As a user, I can view only my own wallets/accounts.
4. As portfolio service, I validate whether account is attachable before linking.
5. As operations team, I can rely on deterministic audit trail for wallet/account lifecycle events.

## 6. Diagram Set Selection

Selected because this is a Tier-3 complex microservice:
1. Use case diagram (multiple actors/services).
2. Class/domain model (persistent entities and invariants).
3. Flow diagrams per major use case:
   - State diagram
   - Activity diagram
   - Sequence diagram

## 7. Open Questions And Risks

Open questions:
1. Should internal validation API be synchronous-only or add cache hint contract?
2. Should archived wallet allow address read for audit-only UI?
3. Exact TTL for idempotency records per environment.

Risks:
1. Policy path drift between auth seed and controller mappings.
2. Outbox relay delay can cause temporary portfolio staleness.
3. Missing unique constraints can produce duplicate active accounts.
