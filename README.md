# mangala-wallet-service

Wallet and address domain service for Mangala backend.

## Design Docs (v1)

1. `docs/design/wallet-service-v1/design-overview.md`
2. `docs/design/wallet-service-v1/api-contract-v1.md`
3. `docs/design/wallet-service-v1/microservice-detailed-design.md`
4. `docs/design/wallet-service-v1/diagrams.md`

## Implementation Note

Design follows current system high-level architecture:
1. Gateway enforces authn/authz and routes wallet APIs.
2. Wallet service owns wallet/address write model.
3. Downstream services consume wallet events asynchronously.
