# Wallet Service v1 Diagrams

## 1. Use Case Diagram

```plantuml
@startuml
left to right direction
actor "End User" as User
actor "API Gateway" as Gateway
actor "Portfolio Service" as Portfolio
actor "Auth/Policy Service" as Auth

rectangle "Wallet Service v1" {
  usecase "Create Wallet" as UC1
  usecase "Create Address/Account" as UC2
  usecase "List Wallets/Addresses" as UC3
  usecase "Archive Wallet" as UC4
  usecase "Validate Account for Portfolio" as UC5
  usecase "Publish Wallet Events" as UC6
}

User --> UC1
User --> UC2
User --> UC3
User --> UC4
Portfolio --> UC5
UC1 .> UC6 : <<include>>
UC2 .> UC6 : <<include>>
UC4 .> UC6 : <<include>>
Gateway --> UC1
Gateway --> UC2
Gateway --> UC3
Gateway --> UC4
Auth --> Gateway
@enduml
```

## 2. Class Diagram

```plantuml
@startuml
class Wallet {
  +id: UUID
  +userId: UUID
  +name: String
  +networkCode: String
  +status: WalletStatus
  +createdAt: Instant
  +updatedAt: Instant
  +deletedAt: Instant
}

class Address {
  +id: UUID
  +walletId: UUID
  +userId: UUID
  +networkCode: String
  +address: String
  +addressFormat: String
  +label: String
  +isDefault: Boolean
  +status: AddressStatus
  +createdAt: Instant
}

class Network {
  +id: UUID
  +code: String
  +chainId: Long
  +family: String
  +name: String
  +nativeSymbol: String
  +nativeDecimals: Integer
  +explorerBaseUrl: String
  +addressValidatorType: String
  +isTestnet: Boolean
  +status: NetworkStatus
  +createdAt: Instant
  +updatedAt: Instant
}

class IdempotencyRecord {
  +id: UUID
  +key: String
  +userId: UUID
  +endpoint: String
  +requestHash: String
  +responseSnapshot: String
  +expiresAt: Instant
}

class OutboxEvent {
  +id: UUID
  +aggregateType: String
  +aggregateId: UUID
  +eventType: String
  +payload: String
  +createdAt: Instant
  +publishedAt: Instant
}

enum WalletStatus {
  ACTIVE
  ARCHIVED
}

enum AddressStatus {
  ACTIVE
  DEPRECATED
}

enum NetworkStatus {
  ACTIVE
  DEPRECATED
  DISABLED
}

Wallet "1" -- "0..*" Address
Network "1" -- "0..*" Wallet
Network "1" -- "0..*" Address
Wallet "1" -- "0..*" OutboxEvent
Address "1" -- "0..*" OutboxEvent
Wallet ..> IdempotencyRecord
Address ..> IdempotencyRecord
Wallet --> WalletStatus
Address --> AddressStatus
Network --> NetworkStatus
@enduml
```

## 3. Flow A - Create Wallet + Account

### 3.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> WALLET_PENDING
    WALLET_PENDING --> WALLET_ACTIVE: Wallet created
    WALLET_ACTIVE --> ACCOUNT_PENDING: Create address/account requested
    ACCOUNT_PENDING --> ACCOUNT_ACTIVE: Address created and validated
    ACCOUNT_ACTIVE --> WALLET_ARCHIVED: Wallet archived
    WALLET_ARCHIVED --> WALLET_ARCHIVED: Repeat archive (idempotent)
```

### 3.2 Activity Diagram

```mermaid
flowchart TD
    A["Receive POST /api/v1/wallets"] --> B["Validate JWT context and payload"]
    B --> C{"Network supported?"}
    C -->|No| D["Return 422 UNSUPPORTED_NETWORK"]
    C -->|Yes| E["Persist wallet + outbox event in transaction"]
    E --> F["Return 201 wallet"]
    F --> G["Receive POST /api/v1/addresses"]
    G --> H["Validate wallet ownership and network consistency"]
    H --> I{"Address unique for user+network?"}
    I -->|No| J["Return 409 DUPLICATE_ACCOUNT"]
    I -->|Yes| K["Persist address + outbox event"]
    K --> L["Return 201 address"]
```

### 3.3 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Gateway
    participant WalletService
    participant WalletDB
    participant Kafka

    Client->>Gateway: POST /api/v1/wallets
    Gateway->>Gateway: JWT + wallets:create check
    Gateway->>WalletService: Forward request with user context
    WalletService->>WalletDB: Insert wallet + outbox (tx)
    WalletDB-->>WalletService: Commit
    WalletService->>Kafka: Publish wallet.created
    WalletService-->>Gateway: 201 Wallet
    Gateway-->>Client: 201

    Client->>Gateway: POST /api/v1/addresses
    Gateway->>WalletService: Forward request
    WalletService->>WalletDB: Validate ownership/network + insert address + outbox
    WalletService->>Kafka: Publish address.created
    WalletService-->>Gateway: 201 Address
    Gateway-->>Client: 201
```

## 4. Flow B - Attach Account To Portfolio

### 4.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> DETACHED
    DETACHED --> VALIDATING: Attach requested
    VALIDATING --> ATTACHED: Validation OK
    VALIDATING --> DETACHED: Validation failed
    ATTACHED --> DETACHED: Account deprecated/removed
```

### 4.2 Activity Diagram

```mermaid
flowchart TD
    A["Portfolio receives attach-account command"] --> B["Call wallet internal validate API"]
    B --> C{"valid == true?"}
    C -->|No| D["Reject attach with reasonCode"]
    C -->|Yes| E["Persist portfolio-account link"]
    E --> F["Publish portfolio.account_attached"]
    F --> G["Return 201 Created"]
```

### 4.3 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Gateway
    participant PortfolioService
    participant WalletService
    participant PortfolioDB

    Client->>Gateway: POST /api/v1/portfolios/{id}/accounts
    Gateway->>PortfolioService: Forward command + user context
    PortfolioService->>WalletService: POST /api/v1/internal/accounts/validate
    WalletService-->>PortfolioService: valid/reasonCode/status
    alt valid
      PortfolioService->>PortfolioDB: Insert portfolio account link
      PortfolioService-->>Gateway: 201 Created
      Gateway-->>Client: 201 Created
    else invalid
      PortfolioService-->>Gateway: 422 + reasonCode
      Gateway-->>Client: 422
    end
```

## 5. Flow C - Multi-Network Validation

### 5.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> PENDING_CHECK
    PENDING_CHECK --> VALID_ETH: network=ETH and format valid
    PENDING_CHECK --> VALID_BSC: network=BSC and format valid
    PENDING_CHECK --> VALID_SOL: network=SOL and format valid
    PENDING_CHECK --> INVALID: unsupported network or bad format
```

### 5.2 Activity Diagram

```mermaid
flowchart TD
    A["Receive create/validate request"] --> B{"network in [ETH,BSC,SOL]?"}
    B -->|No| C["Return 422 UNSUPPORTED_NETWORK"]
    B -->|Yes| D["Run network-specific address format validator"]
    D --> E{"Format valid?"}
    E -->|No| F["Return 422 INVALID_ADDRESS_FORMAT"]
    E -->|Yes| G["Check ownership and active status"]
    G --> H{"Ownership and status pass?"}
    H -->|No| I["Return 403/422 by violation type"]
    H -->|Yes| J["Return success"]
```

### 5.3 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant PortfolioService
    participant WalletService
    participant WalletDB

    PortfolioService->>WalletService: Validate account(network,address,walletId,userId)
    WalletService->>WalletService: Check supported network
    WalletService->>WalletService: Validate address format by network
    WalletService->>WalletDB: Load wallet + address + status + ownership
    WalletDB-->>WalletService: Records
    WalletService-->>PortfolioService: valid/reasonCode
```

## 6. Flow D - Network Catalog Sync To Portfolio

### 6.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> NETWORK_ACTIVE
    NETWORK_ACTIVE --> NETWORK_DEPRECATED: status changed to DEPRECATED
    NETWORK_DEPRECATED --> NETWORK_DISABLED: status changed to DISABLED
    NETWORK_DISABLED --> NETWORK_ACTIVE: re-enabled by admin
```

### 6.2 Activity Diagram

```mermaid
flowchart TD
    A["Network metadata updated in wallet-service"] --> B["Write network row + outbox event in tx"]
    B --> C["Outbox relay publishes network.status_changed.v1"]
    C --> D["Portfolio consumer receives event"]
    D --> E["Update local network read model"]
    E --> F["Mark affected attach flows with latest network status"]
```

### 6.3 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant WalletAdmin
    participant WalletService
    participant WalletDB
    participant Kafka
    participant PortfolioService
    participant PortfolioDB

    WalletAdmin->>WalletService: Update network status (ACTIVE->DEPRECATED)
    WalletService->>WalletDB: Update network + outbox event (tx)
    WalletDB-->>WalletService: Commit
    WalletService->>Kafka: Publish network.status_changed.v1
    Kafka-->>PortfolioService: Consume event
    PortfolioService->>PortfolioDB: Update network read model
    PortfolioService-->>PortfolioService: Enforce new attach validation behavior
```
