# Wallet Service v1 - Detailed Microservice Design

## 1. Service Purpose

Wallet Service v1 provides account source-of-truth for users:
1. Wallet lifecycle management.
2. Address/account lifecycle management.
3. Network catalog management.
4. Account attach validation for portfolio integration.

This service is write-authoritative for wallet/account and network metadata used by downstream services.

## 2. Domain Boundary (DDD-Oriented)

### 2.1 Bounded Context
`Wallet Account Context`

Owned aggregates:
1. `Wallet`
2. `Address`
3. `Network` (catalog metadata)

### 2.2 Responsibilities

Wallet Service owns:
1. Wallet/account creation/update/archive rules.
2. Address format validation by network.
3. Uniqueness and ownership constraints.
4. Publish lifecycle events to Kafka.
5. Internal validation contract for account attach.

Wallet Service does not own:
1. Portfolio composition, allocation, P&L.
2. Price feed and market data.
3. Transfer execution and blockchain transaction processing.

### 2.3 Integration Boundaries

1. Upstream:
   - API Gateway provides authn/authz and context headers.
2. Downstream:
   - Portfolio service consumes events and performs attach flow.
   - History and notification consume lifecycle events.

## 3. Database Strategy

## 3.1 Storage Pattern

Single writer model with PostgreSQL transactional DB + Outbox:
1. Business data and outbox event are committed in one transaction.
2. Async relay publishes outbox rows to Kafka.
3. Consumer services maintain their own read models.

## 3.2 Table Design (Logical)

### `networks`
1. `id` UUID PK
2. `code` varchar(32) unique (`ETH`, `BSC`, `SOL`, ...)
3. `chain_id` bigint nullable
4. `family` varchar(32) (`EVM`, `SVM`, ...)
5. `name` varchar(120)
6. `native_symbol` varchar(16)
7. `native_decimals` smallint
8. `explorer_base_url` varchar(500)
9. `address_validator_type` varchar(64)
10. `is_testnet` boolean
11. `status` varchar(32) (`ACTIVE`, `DEPRECATED`, `DISABLED`)
12. `created_at`, `updated_at`

Indexes:
1. `uk_networks_code(code)`
2. `idx_networks_status(status)`

### `wallets`
1. `id` UUID PK
2. `user_id` UUID not null
3. `network_code` varchar(32) FK -> `networks.code`
4. `name` varchar(120)
5. `status` varchar(32) (`ACTIVE`, `ARCHIVED`)
6. `created_at`, `updated_at`, `deleted_at`

Indexes:
1. `idx_wallets_user_status_network(user_id, status, network_code)`
2. `idx_wallets_user_created_at(user_id, created_at desc)`
3. Optional unique for active wallet names:
   - partial unique `(user_id, network_code, lower(name)) where status='ACTIVE'`

### `addresses`
1. `id` UUID PK
2. `wallet_id` UUID FK -> `wallets.id`
3. `user_id` UUID not null (denormalized for fast ownership filtering)
4. `network_code` varchar(32) FK -> `networks.code`
5. `address` varchar(255)
6. `address_normalized` varchar(255)
7. `label` varchar(120)
8. `is_default` boolean
9. `status` varchar(32) (`ACTIVE`, `DEPRECATED`)
10. `created_at`, `updated_at`

Indexes:
1. `idx_addresses_wallet_status(wallet_id, status)`
2. `idx_addresses_user_network_status(user_id, network_code, status)`
3. partial unique `(user_id, network_code, address_normalized) where status='ACTIVE'`

### `idempotency_records`
1. `id` UUID PK
2. `idempotency_key` varchar(128)
3. `user_id` UUID
4. `endpoint` varchar(200)
5. `request_hash` varchar(128)
6. `response_status` int
7. `response_body` jsonb
8. `expires_at` timestamp
9. `created_at`

Indexes:
1. unique `(idempotency_key, user_id, endpoint)`
2. `idx_idempotency_expires_at(expires_at)`

### `outbox_events`
1. `id` UUID PK
2. `aggregate_type` varchar(64)
3. `aggregate_id` UUID
4. `event_type` varchar(128)
5. `event_version` int
6. `payload` jsonb
7. `created_at`
8. `published_at` nullable

Indexes:
1. `idx_outbox_unpublished(created_at) where published_at is null`

## 3.3 Concurrency Rules

1. Update/archive operations use optimistic lock version field (recommended) or row lock where required.
2. Default account switching inside one wallet must be transaction-safe.
3. Idempotent create must replay original response for same key+hash.

## 4. Network Management Strategy

## 4.1 Why Not Enum-Only

Enum-only fails when network metadata evolves:
1. chain-specific validators change.
2. explorer/RPC details change.
3. deprecate/disable network without redeploy all services.

## 4.2 Network Catalog Ownership

Wallet Service owns the `networks` catalog and exposes:
1. Public read APIs for clients.
2. Internal read/sync APIs for portfolio and other services.

## 4.3 Portfolio Handling

Portfolio service should store:
1. `network_code` on portfolio-account and holdings records.
2. Snapshot metadata (`family`, `native_symbol`, `decimals`) for read performance.

Portfolio should not own authoritative network definition; it consumes from wallet service via API/event.

## 5. API Contract Extensions (Detailed)

## 5.1 Public APIs
1. `GET /api/v1/networks`
   - Filter by `status`, `family`, `isTestnet`.
2. `GET /api/v1/networks/{code}`
3. Existing wallet/address public APIs.

## 5.2 Internal APIs
1. `POST /api/v1/internal/accounts/validate`
   - Validate user ownership + status + network compatibility.
2. `GET /api/v1/internal/networks`
   - Lightweight network metadata for sync/read model.

## 5.3 Validation Response Contract
Stable response:
1. `valid` boolean
2. `reasonCode` enum
3. `walletStatus`, `addressStatus`
4. `networkCode`, `networkFamily`
5. optional `details` map

Reason codes:
1. `OK`
2. `WALLET_NOT_FOUND`
3. `ADDRESS_NOT_FOUND`
4. `OWNERSHIP_MISMATCH`
5. `NETWORK_MISMATCH`
6. `WALLET_ARCHIVED`
7. `ADDRESS_DEPRECATED`
8. `NETWORK_DEPRECATED`
9. `NETWORK_DISABLED`
10. `INVALID_ADDRESS_FORMAT`

## 6. Event Contract

## 6.1 Wallet/Address Domain Events
1. `wallet.created.v1`
2. `wallet.updated.v1`
3. `wallet.archived.v1`
4. `address.created.v1`
5. `address.updated.v1`
6. `address.deprecated.v1`

## 6.2 Network Catalog Events
1. `network.created.v1`
2. `network.updated.v1`
3. `network.status_changed.v1`

## 6.3 Event Envelope
```json
{
  "eventId": "f42f0c4f-8f52-4b5f-98fa-8c2e7f0dcf8e",
  "eventType": "address.created.v1",
  "eventVersion": 1,
  "occurredAt": "2026-02-24T03:00:00Z",
  "producer": "wallet-service",
  "traceId": "c822f6d7f8a944f0",
  "payload": {}
}
```

## 7. Flow Policies And Invariants

1. `address.network_code == wallet.network_code`.
2. `address.user_id == wallet.user_id`.
3. Only ACTIVE wallet can accept new ACTIVE addresses.
4. One ACTIVE default address per wallet per network (if business requires default).
5. Archived wallet cannot be attached to new portfolio entries.

## 8. Non-Functional Baseline

1. SLO target:
   - p95 read APIs < 150ms
   - p95 write APIs < 300ms
2. Retry policy:
   - idempotent reads: safe retries
   - creates: retries only with idempotency key.
3. Observability:
   - metrics by endpoint + reasonCode.
   - audit logs for create/archive/validation failures.

## 9. Implementation Phasing

1. Phase 1:
   - Networks read APIs + wallet/address core write model + outbox.
2. Phase 2:
   - Internal validate API + portfolio attach integration tests.
3. Phase 3:
   - Network status events + portfolio sync optimizations + operational hardening.
