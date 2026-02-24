# Wallet Service v1 API Contract (Draft)

## Endpoints

### Network APIs
1. `GET /api/v1/networks`
2. `GET /api/v1/networks/{code}`

### Wallet APIs
1. `GET /api/v1/wallets`
2. `POST /api/v1/wallets`
3. `GET /api/v1/wallets/{walletId}`
4. `PUT /api/v1/wallets/{walletId}`
5. `DELETE /api/v1/wallets/{walletId}`

### Address APIs
1. `GET /api/v1/addresses`
2. `GET /api/v1/addresses/{addressId}`
3. `POST /api/v1/addresses`

### Internal Integration APIs (Portfolio-facing)
1. `POST /api/v1/internal/accounts/validate`
   - Purpose: validate account can be attached into portfolio.
   - Caller: internal trusted service/gateway only.
2. `GET /api/v1/internal/networks`
   - Purpose: sync network metadata for portfolio read model.
   - Caller: internal trusted service only.

## Authorization Mapping

1. Wallet read: `wallets:read`
2. Wallet create: `wallets:create`
3. Wallet update: `wallets:update`
4. Wallet delete: `wallets:delete`
5. Address read: `addresses:read`
6. Address create: `addresses:create`

## Request/Response Baseline

### Network Response

```json
{
  "code": "ETH",
  "name": "Ethereum Mainnet",
  "family": "EVM",
  "chainId": 1,
  "nativeSymbol": "ETH",
  "nativeDecimals": 18,
  "explorerBaseUrl": "https://etherscan.io",
  "status": "ACTIVE",
  "isTestnet": false
}
```

### Create Wallet Request

```json
{
  "name": "Main wallet",
  "network": "ETH"
}
```

### Wallet Response

```json
{
  "id": "f7fcfa0f-2e10-4e7b-9590-c2eb9aa6edfa",
  "userId": "0e1e4a0c-9b35-4f6d-a1ff-ea4db70ff6ab",
  "name": "Main wallet",
  "network": "ETH",
  "status": "ACTIVE",
  "createdAt": "2026-02-23T04:00:00Z",
  "updatedAt": "2026-02-23T04:00:00Z"
}
```

### Paged List Response

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Validate Account Request (Internal)

```json
{
  "userId": "0e1e4a0c-9b35-4f6d-a1ff-ea4db70ff6ab",
  "walletId": "f7fcfa0f-2e10-4e7b-9590-c2eb9aa6edfa",
  "addressId": "ef5d6d8d-3b89-43fb-8dfe-7727f3a89d44",
  "network": "ETH"
}
```

### Validate Account Response (Internal)

```json
{
  "valid": true,
  "reasonCode": "OK",
  "walletStatus": "ACTIVE",
  "addressStatus": "ACTIVE",
  "network": "ETH",
  "networkFamily": "EVM"
}
```

## Error Contract

```json
{
  "type": "https://mangala.dev/errors/wallet-validation-failed",
  "title": "Validation failed",
  "status": 422,
  "code": "WALLET_VALIDATION_FAILED",
  "detail": "network must be one of [ETH, BSC, SOL]",
  "instance": "/api/v1/wallets",
  "traceId": "a4853eec76f14cde"
}
```

## Behavioral Rules

1. `Idempotency-Key` supported for:
   - `POST /api/v1/wallets`
   - `POST /api/v1/addresses`
2. `DELETE /api/v1/wallets/{walletId}` performs soft delete.
3. Ownership is always derived from authenticated principal, never from request body.
4. All timestamps use UTC ISO-8601 format.
5. Address network must match wallet network.
6. Uniqueness baseline:
   - Recommended unique `(userId, network, address)` for active addresses.
7. Internal validation API must return deterministic `reasonCode`:
   - `OK`
   - `WALLET_NOT_FOUND`
   - `ADDRESS_NOT_FOUND`
   - `OWNERSHIP_MISMATCH`
   - `NETWORK_MISMATCH`
   - `WALLET_ARCHIVED`
   - `ADDRESS_DEPRECATED`
   - `NETWORK_DEPRECATED`
   - `NETWORK_DISABLED`
   - `INVALID_ADDRESS_FORMAT`

## Multi-Network Support Matrix

Supported networks in MVP:
1. `ETH` (EVM)
2. `BSC` (EVM)
3. `SOL` (non-EVM)

Validation implications:
1. Network is mandatory in wallet creation.
2. Address format validator is network-specific.
3. Account attach to portfolio must pass same-network validation.
4. Network metadata is sourced from network catalog, not hard-coded enum only.

## Portfolio Integration Contract

1. Event contract (async):
   - `wallet.created`
   - `wallet.updated`
   - `wallet.archived`
   - `address.created`
2. Validation contract (sync):
   - Portfolio calls internal validate endpoint before attaching account.
   - Portfolio syncs network metadata from internal networks endpoint.
3. Idempotency:
   - Repeated attach attempts should rely on stable validation result and idempotent portfolio command handling.
