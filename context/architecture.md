# Project Architecture & Component Distribution

## High-Level Architecture
- **Paradigm**: Microservices / Hexagonal Architecture (Ports and Adapters).
- **Core Technology Stack**: Java 21 / Spring Boot 3.x / Spring WebFlux (Reactive).
- **Source Code Distribution**:
  - `src/main/java/com/aegis/sign/domain`: Pure Domain Logic (Entities, Rules, Value Objects).
  - `src/main/java/com/aegis/sign/application`: Use Cases and Ports (Input/Output interfaces).
  - `src/main/java/com/aegis/sign/infrastructure`: External Adapters (REST, PostgreSQL, Redis, MinIO, PKI).
  - `src/main/resources`: Configuration (application.yml) and Database Migrations.

## Component Layers
### 1. Presentation Layer (UI/Interface)
- **Client/Web**: The microservice exposes a REST API. A separate frontend (not in this repo) is expected to consume it.
- **Technology**: Spring WebFlux (REST Controllers).
- **Static Assets**: None (API-only service).

### 2. Business Layer (Logic)
- **Services/Controllers**: Application Services implement the Use Cases defined in the ports.
- **Validation**: Domain-level validation within Entities and Value Objects.
- **Coordination**: Reactive orchestration of KYC and Signature flows.

### 3. Data/Persistence Layer
- **Data Access Pattern**: Repository Pattern (Output Ports).
- **ORM/Driver**: R2DBC (Reactive Relational Database Connectivity) for PostgreSQL.
- **Schema Management**: Flyway or Liquibase (intended).

## Full Request/Data Lifecycle
```mermaid
graph TD
    Client[User/System] -- REST API --> EntryPoint[WebFlux Controller]
    EntryPoint -- Command/Query --> InPort[Input Port / Use Case]
    InPort -- Execute --> Service[Application Service]
    Service -- Domain Logic --> Domain[Domain Entities]
    Service -- Persist/Query --> OutPort[Output Port / Repository Interface]
    OutPort -- Call --> Adapter[Infrastructure Adapter]
    Adapter -- SQL/Command --> Storage[(PostgreSQL/Redis/MinIO)]
    Storage -- Result --> Adapter
    Adapter -- DTO/Entity --> OutPort
    OutPort -- Data --> Service
    Service -- Result --> InPort
    InPort -- Response --> EntryPoint
    EntryPoint -- JSON --> Client
```

## System Integration Points
- **Internal Modules**: 
    - **KYC Module**: Handles document intake and identity verification.
    - **Signature Module**: Manages hashing, signing, and audit trails.
    - **Template Module**: Assembles contracts from reusable clauses.
- **External Services (Infrastructure Adapters)**:
    - **PostgreSQL (R2DBC)**: Transactional storage.
    - **Redis**: Temporary state for KYC sessions and OTPs.
    - **MinIO**: Object storage for documents.
    - **OCR Engine (Adapter)**: Encapsulates Tesseract / PDFBox to provide text extraction services to the domain.
    - **Biometrics Engine (Adapter)**: Encapsulates OpenCV / DeepFace to provide face matching services to the domain.
    - **PKI Adapter**: Interfaces with internal certificate management for digital signing.

---

### Context & Navigation
- [GEMINI.md](../../GEMINI.md)
- [business_logic.md](business_logic.md)
- [database.md](database.md)
