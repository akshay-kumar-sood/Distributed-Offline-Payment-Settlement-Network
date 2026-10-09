UPI Offline Mesh Demo
A Spring Boot demo of offline, mesh-routed payments with deferred settlement. Virtual phones relay encrypted payment packets until a bridge device reconnects to the internet and uploads them to the backend.
This project simulates the mesh in software. It is a learning/portfolio demo, not a real UPI integration.

Features
- Hybrid encryption: RSA-OAEP + AES-256-GCM
- Duplicate-payment protection using SHA-256 hashes and atomic idempotency checks
- Replay protection with a 24-hour freshness check and unique nonces
- Transactional debit/credit and transaction ledger
- Interactive dashboard to simulate sending, gossiping, and uploading packets
- Concurrency and tamper-detection tests
Requirements
- JDK 17+
- No separate Maven installation required; the Maven Wrapper is included.
- H2 in-memory database (configured by the project)
Run
Windows (PowerShell):
.\mvnw.cmd spring-boot:run
macOS/Linux:
./mvnw spring-boot:run
Open http://localhost:8080 to use the dashboard.
Run tests:
.\mvnw.cmd test
Demo flow
1. Inject a payment from the dashboard.
2. Run gossip rounds to distribute the encrypted packet across virtual devices.
3. Flush bridges to upload packets to the backend.
4. View account balances and the transaction ledger. Duplicate packets should settle only once.
Architecture
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
API
Method	Endpoint	Purpose
GET	/	Demo dashboard
GET	/api/server-key	Server public key
GET	/api/accounts	Account balances
GET	/api/transactions	Recent transactions
GET	/api/mesh/state	Virtual device state
POST	/api/demo/send	Create and inject a demo payment
POST	/api/mesh/gossip	Run a mesh gossip round
POST	/api/mesh/flush	Upload packets from bridge devices
POST	/api/mesh/reset	Reset mesh and idempotency state
POST	/api/bridge/ingest	Ingest an uploaded packet
GET	/h2-console	H2 database console


H2 console JDBC URL: jdbc:h2:mem:upimesh
Username: sa · Password: (blank)
Tests
.\mvnw.cmd test
Tests cover encryption/decryption, tampered-ciphertext rejection, and concurrent duplicate delivery (one settlement only).
Limitations
- The mesh is simulated; real Bluetooth/Wi-Fi Direct communication is not implemented.
- The in-memory database and idempotency cache are not durable or shared across server instances.
- This demo does not integrate with NPCI, banks, real UPI accounts, or production authentication.
- Offline payments cannot guarantee funds or prevent double-spending without additional wallet and settlement mechanisms.
