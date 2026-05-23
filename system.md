# System Overview, Technologies & Setup

## Overview
`aegis-sign` is a self-hosted microservice for Identity Verification (KYC) and Advanced Electronic Signature (FEA). It aims to provide full control over sensitive biometric data and legal documents without relying on external SaaS providers.

## Full Tech Stack & Versions
- **Core Language**: Java 21 (LTS).
- **Runtime/Platform**: Spring Boot 3.x with Spring WebFlux.
- **Frameworks**: Project Reactor (Reactive), Spring Data R2DBC.
- **Database Server**: PostgreSQL 16 (Relational), Redis 7 (Cache).
- **Object Storage**: MinIO (S3 API compatible).
- **Build/Package Tool**: Maven (mvnw provided).
- **Core Libraries**: 
    - OCR: Tesseract / Apache PDFBox.
    - Biometrics: OpenCV / DeepFace (intended).
    - PDF: OpenPDF / iText.

## External Integrations & Service Dependencies
- **PostgreSQL**: Stores Audit Trails, contract metadata, and KYC results.
- **Redis**: Manages short-lived KYC sessions and rate limiting.
- **MinIO**: Stores the actual PDF documents and identity images.
- **Internal PKI**: Generates and manages X.509 certificates for PAdES signatures.

## Environment Setup & Prerequisites
- **Operating System**: Linux (preferred for production), macOS/Windows for development.
- **System Requirements**: 
    - Docker & Docker Compose installed.
    - Java 21 JDK.
- **Local Dev Tools**: 
    - IDE with Lombok support.
    - Postman / Insomnia for API testing.

## Full Installation & Running Steps
1. **Infra Setup**: Run `docker compose up -d` to start PostgreSQL, Redis, and MinIO.
2. **Build**: Run `./mvnw clean install`.
3. **Run**: Run `./mvnw spring-boot:run`.
4. **Verify**: Check `http://localhost:8080/actuator/health` (once implemented).

## Key Operational Commands
| Command / Script | Purpose | Context/Module |
|------------------|---------|----------------|
| `./mvnw clean verify` | Compile and run all tests | Root |
| `docker compose up -d` | Start infrastructure | Root |

---

### Context & Navigation
- [GEMINI.md](GEMINI.md)
- [AGENTS.md](AGENTS.md)
- [context/architecture.md](context/architecture.md)
