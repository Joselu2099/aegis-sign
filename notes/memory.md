# Project Memory, Backlog & Learning Log

## Current Session Context & Active Goal
- **Status**: Initialization / Scaffolding.
- **Current Objective**: Establish the project context documentation and initial structure.
- **Environment**: macOS, Java 21, Spring Boot 3.x.

## Knowledge Base (What I've Learned)
- **Project Purpose**: Self-hosted KYC and Electronic Signature service to replace third-party providers.
- **Architecture**: Hexagonal Architecture with Reactive programming (Spring WebFlux).
- **Tech Stack**: Java 21, PostgreSQL, Redis, MinIO, OpenCV/Tesseract.
- **Contract System**: Modular assembly using Templates and Clauses.
- **Consent Mechanism**: Signatures require OTP validation after a successful KYC session.

## Project Backlog (TODOs & Future Work)
| Priority | Status | Task / Requirement | Context Link |
|----------|--------|-------------------|--------------|
| High     | TODO   | Scaffold project structure (Maven/Gradle) | - |
| High     | TODO   | Setup Docker Compose for Infra | README.md |
| High     | TODO   | Implement Template & Clause Domain | [TemplateModule] |
| High     | TODO   | Implement KycSession Domain & API | [Módulo KYC] |
| Medium   | TODO   | Implement OCR Pipeline (Tesseract Adapter) | [OCR Engine] |
| Medium   | TODO   | Implement Biometric Adapter (OpenCV) | [Biometrics Engine] |
| Medium   | TODO   | Implement Signature Hashing & PDF Rendering | [Módulo Firma] |

## Significant Decisions Log
| Date | Technical Decision | Rationale & Trade-offs | Related Code |
|------|--------------------|------------------------|--------------|
| 2026-05-23 | Hexagonal Architecture | To decouple domain logic from infrastructure (MinIO, Redis, OCR engines). | - |
| 2026-05-23 | Reactive Stack | To handle high concurrency and large file streams without blocking. | - |
| 2026-05-23 | Modular Contracts | Templates/Clauses allow for dynamic document generation without modifying code. | [TemplateModule] |

## Legacy Conventions & Anti-patterns
- **No Third-Party APIs**: Avoid using cloud-based OCR or Biometrics; all must be local/self-hosted.

---

### Navigation
- [GEMINI.md](../GEMINI.md)
- [business_logic.md](../context/business_logic.md)
- [database.md](../context/database.md)
