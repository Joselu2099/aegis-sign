# Project Memory, Backlog & Learning Log

## Current Session Context & Active Goal
- **Status**: Completed / Release and Verification.
- **Current Objective**: Document and verify the fully implemented reactive KYC and advanced signature microservice.
- **Environment**: macOS, Java 21, Spring Boot 3.x, PostgreSQL, Redis, MinIO.

## Knowledge Base (What I've Learned)
- **Project Purpose**: Self-hosted KYC and Advanced Electronic Signature (FEA) service.
- **Architecture**: Hexagonal Architecture with Reactive programming (Spring WebFlux & R2DBC).
- **Tech Stack**: Java 21, PostgreSQL, Redis, MinIO, OpenPDF.
- **Verification Engine**: Biometric comparison (facial 1:1 score verification) and OCR document parsing (ICAO Doc 9303 checksum validation) implemented.
- **Auditing**: Audit trail generated in JSON structure and preserved with signer metadata.

## Project Backlog (TODOs & Future Work)
| Priority | Status | Task / Requirement | Context Link |
|----------|--------|-------------------|--------------|
| High     | DONE   | Initialize Database Schema (V1 Migration) | [PostgreSQL] |
| High     | DONE   | Scaffold project structure (Maven/Gradle) | - |
| High     | DONE   | Setup Docker Compose for Infra | README.md |
| High     | DONE   | Implement Template Compilation & Rendering | [Módulo Firma] |
| High     | DONE   | Implement KycSession Domain & API | [Módulo KYC] |
| Medium   | DONE   | Implement OCR & MRZ Validation (ICAO Doc 9303) | [OCR Engine] |
| Medium   | DONE   | Implement Biometric Facial Matching | [Biometrics Engine] |
| Medium   | DONE   | Implement Signature Hashing & PAdES Seal | [Módulo Firma] |
| Medium   | DONE   | Implement Audit Trail Generation | [Módulo Firma] |
| Low      | TODO   | Implement local Docker daemon support for Integration Tests | requirements.md |

## Significant Decisions Log
| Date | Technical Decision | Rationale & Trade-offs | Related Code |
|------|--------------------|------------------------|--------------|
| 2026-05-23 | Hexagonal Architecture | To decouple domain logic from infrastructure (MinIO, Redis, OCR engines). | - |
| 2026-05-23 | Reactive Stack | To handle high concurrency and large file streams without blocking. | - |
| 2026-05-23 | In-Memory Rate Limiting | Implemented Token Bucket filter locally; Redis remains configured as cache backend. | TokenBucketRateLimiterFilter |
| 2026-05-23 | Ephemeral Storage | Ephemeral KYC sessions are handled dynamically with standard expiry in PostgreSQL. | KycRepositoryAdapter |

## Legacy Conventions & Anti-patterns
- **No Third-Party APIs**: Avoid using cloud-based OCR or Biometrics; all must be local/self-hosted.

---

### Navigation
- [GEMINI.md](../GEMINI.md)
- [business_logic.md](../context/business_logic.md)
- [database.md](../context/database.md)
