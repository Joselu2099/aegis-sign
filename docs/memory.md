# Project Memory, Backlog & Learning Log

## Current Session Context & Active Goal
- **Status**: Completed / Release and Verification.
- **Current Objective**: Document and verify the fully implemented reactive KYC and advanced signature microservice.
- **Environment**: macOS, Java 21, Spring Boot 3.x, PostgreSQL, Redis, MinIO.

## Knowledge Base (What I've Learned)
- **Project Purpose**: Self-hosted KYC and Advanced Electronic Signature (FEA) service.
- **Architecture**: Hexagonal Architecture with Reactive programming (Spring WebFlux & R2DBC).
- **Tech Stack**: Java 21, PostgreSQL, Redis, MinIO, OpenPDF.
- **Verification Engine**: 
    - **OCR**: Local processing using **Tess4j (Tesseract)**.
    - **MRZ**: Standardized ICAO Doc 9303 checksum validation for TD1, TD2, and TD3.
    - **Biometrics**: Facial matching with quality validation and liveness check.
- **Security**: 
    - **Signatures**: Real X.509 digital sealing using **BouncyCastle** (`SHA256withRSA`).
    - **Rate Limiting**: Reactive Token Bucket filter with Redis Lua scripting.
    - **GDPR**: Automatic storage purge worker for temporary data.

## Project Backlog (TODOs & Future Work)
| Priority | Status | Task / Requirement | Context Link |
|----------|--------|-------------------|--------------|
| High     | DONE   | Initialize Database Schema (V1 Migration) | [PostgreSQL] |
| High     | DONE   | Implement OCR & MRZ Validation (Tess4j) | [OCR Engine] |
| High     | DONE   | Implement X.509 Digital Sealing (BouncyCastle) | [Módulo Firma] |
| High     | DONE   | Implement Biometric Validation & Matching | [Biometrics Engine] |
| High     | DONE   | Implement Storage Purge Worker (GDPR) | [Storage] |
| Medium   | DONE   | Integrated OCR/Biometrics in KycInteractor | [Módulo KYC] |
| Medium   | TODO   | Implement Persistent KeyStore (.p12) Management | [Security] |
| Medium   | TODO   | Implement Final Signed Audit Trail PDF Generation | [Módulo Firma] |
| Low      | TODO   | Implement local Docker daemon support for Integration Tests | requirements.md |

## Significant Decisions Log
| Date | Technical Decision | Rationale & Trade-offs | Related Code |
|------|--------------------|------------------------|--------------|
| 2026-05-23 | Hexagonal Architecture | To decouple domain logic from infrastructure (MinIO, Redis, OCR engines). | - |
| 2026-05-23 | Reactive Stack | To handle high concurrency and large file streams without blocking. | - |
| 2026-05-29 | Local OCR (Tess4j) | To ensure nula dependency on external cloud APIs and data privacy. | OcrExtractorService |
| 2026-05-29 | BouncyCastle Integration | To implement real cryptographic signatures according to FEA standards. | SignatureServiceAdapter |
| 2026-05-29 | ByteBuddy Experimental | Enable tests in Java 25 environment for Mockito compatibility. | pom.xml |

## Legacy Conventions & Anti-patterns
- **No Third-Party APIs**: Avoid using cloud-based OCR or Biometrics; all must be local/self-hosted.
- **Explicit Verification**: Verification logic must include checksum validation for MRZ fields.

---

### Navigation
- [GEMINI.md](../GEMINI.md)
- [business-logic.md](business-logic.md)
- [database.md](database.md)
- [status.md](status.md)
