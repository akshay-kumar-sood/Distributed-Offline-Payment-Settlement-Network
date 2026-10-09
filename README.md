# Distributed Payment Settlement Network

A Java Spring Boot project that explores secure payment packet routing and reliable transaction settlement in unreliable network environments. It simulates a device-to-device mesh where encrypted payment packets are relayed to an internet-connected bridge and processed by a central backend.

## Key Features

- **Hybrid Encryption:** RSA-OAEP and AES-256-GCM for payment confidentiality and tamper detection.
- **Mesh Network Simulation:** Relays encrypted packets across virtual devices using gossip rounds.
- **Idempotent Processing:** SHA-256-based duplicate detection with atomic checks.
- **Replay Protection:** Rejects stale payment packets using timestamp validation and unique nonces.
- **Transactional Settlement:** Debits and credits accounts atomically while maintaining a transaction ledger.
- **Concurrency Safety:** Uses atomic operations and optimistic locking to protect against duplicate processing and concurrent updates.
- **Interactive Dashboard:** Visualizes packet propagation, bridge uploads, account balances, and settlement results.

## Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                         SENDER PHONE (offline)                          │
│  PaymentInstruction { sender, receiver, amount, pinHash, nonce, time }  │
│              │                                                          │
│              ▼ encrypt with server's RSA public key                     │
│   MeshPacket { packetId, ttl, createdAt, ciphertext }                   │
└──────────────────────────────────────┬──────────────────────────────────┘
                                       │ Bluetooth gossip
                                       ▼
        ┌─────────┐  hop   ┌─────────┐  hop   ┌─────────┐
        │stranger1│ ─────▶ │stranger2│ ─────▶ │ bridge  │ ◀── walks outside
        └─────────┘        └─────────┘        └────┬────┘     gets 4G
                                                   │
                                                   ▼ HTTPS POST
┌─────────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT BACKEND (this project)                  │
│                                                                         │
│  /api/bridge/ingest                                                     │
│       │                                                                 │
│       ▼                                                                 │
│  [1] hash ciphertext (SHA-256)                                          │
│       │                                                                 │
│       ▼                                                                 │
│  [2] IdempotencyService.claim(hash)  ◀── atomic putIfAbsent (≈ Redis    │
│       │                                  SETNX). Duplicates rejected    │
│       │                                  here, before any work.         │
│       ▼                                                                 │
│  [3] HybridCryptoService.decrypt(ciphertext)                            │
│       │       (RSA-OAEP unwraps AES key, AES-GCM decrypts payload       │
│       │        AND verifies the auth tag — tampering = exception)       │
│       ▼                                                                 │
│  [4] Freshness check: signedAt within last 24h                          │
│       │                                                                 │
│       ▼                                                                 │
│  [5] SettlementService.settle()                                         │
│       @Transactional: debit sender, credit receiver, write ledger       │
│       @Version on Account = optimistic locking (defense in depth)       │
└─────────────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17+ | Core programming language |
| Spring Boot | REST APIs and backend services |
| Spring Data JPA | Persistence and database access |
| H2 Database | In-memory account and transaction storage |
| RSA-OAEP | AES key encryption |
| AES-256-GCM | Payload encryption and integrity verification |
| SHA-256 | Ciphertext hashing and duplicate detection |
| Maven | Build and dependency management |

## Getting Started

### Prerequisites

- JDK 17 or newer
- Git

The Maven Wrapper is included, so a separate Maven installation is not required.

### Run the Application

**Windows (PowerShell)**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080** to access the dashboard.

### Run Tests

**Windows**

```powershell
.\mvnw.cmd test
```

**Linux / macOS**

```bash
./mvnw test
```

The tests cover encryption/decryption, tampered-packet rejection, and concurrent duplicate delivery.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Demo dashboard |
| `GET` | `/api/server-key` | Retrieve the server's public key |
| `GET` | `/api/accounts` | View account balances |
| `GET` | `/api/transactions` | View recent transactions |
| `GET` | `/api/mesh/state` | Inspect virtual device states |
| `POST` | `/api/demo/send` | Create and inject a payment |
| `POST` | `/api/mesh/gossip` | Simulate a mesh gossip round |
| `POST` | `/api/mesh/flush` | Upload packets from bridge devices |
| `POST` | `/api/mesh/reset` | Reset the mesh and idempotency state |
| `POST` | `/api/bridge/ingest` | Process an incoming payment packet |
| `GET` | `/h2-console` | Access the H2 database console |

## Design Highlights

- **Duplicate protection:** An atomic `putIfAbsent` check allows only one concurrent request for a given ciphertext hash to proceed.
- **Authenticated encryption:** AES-GCM detects ciphertext tampering during decryption.
- **Atomic settlement:** `@Transactional` keeps account updates and ledger insertion within a single database transaction.
- **Optimistic locking:** `@Version` helps prevent conflicting concurrent account updates.

## Limitations

This is a proof-of-concept project, not a production payment system.

- Device-to-device mesh communication is simulated; real Bluetooth or Wi-Fi Direct networking is not implemented.
- H2 and the idempotency cache are in-memory and are not shared across backend instances.
- The project does not integrate with real banks, NPCI, or production payment authentication.
- Offline payment authorization and double-spending prevention require additional mechanisms beyond deferred settlement.

## License

Developed for learning and demonstration purposes.
