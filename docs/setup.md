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
2. **Secrets Setup**: Configure and write secrets to HashiCorp Vault (see next section).
3. **Build**: Run `./mvnw clean install`.
4. **Run**: Run `./mvnw spring-boot:run`.
5. **Verify**: Check `http://localhost:8080/actuator/health`.

## Secrets Management with HashiCorp Vault
The application is configured to fetch sensitive configuration (e.g., database passwords, API keys) from HashiCorp Vault. For local development, you can run Vault in a Docker container.

### 1. Run Vault for Development
You can run a Vault instance in dev mode, which is not secure for production but is convenient for local development.

```bash
docker run --cap-add=IPC_LOCK -p 8200:8200 -e 'VAULT_DEV_ROOT_TOKEN_ID=root' --name=dev-vault vault
```

This command starts Vault and sets the root token to `root`.

### 2. Set Environment Variables
The application needs the Vault address and token to connect. Export the following environment variables:

```bash
export VAULT_ADDR='http://127.0.0.1:8200'
export VAULT_TOKEN='root'
```

The application's `application.yml` is configured to use `VAULT_URI` and `VAULT_TOKEN`, so you can set `VAULT_URI` instead of `VAULT_ADDR`. For consistency with the `vault` CLI, `VAULT_ADDR` is recommended.

### 3. Write Secrets to Vault
The application reads secrets from `secret/aegis-sign`. Write the necessary secrets using the `vault` CLI:

```bash
vault kv put secret/aegis-sign \
    db.username="aegis_user" \
    db.password="aegis_password" \
    minio.access-key="aegis_admin" \
    minio.secret-key="aegis_admin_password" \
    keystore.password="changeit" \
    keystore.key-password="changeit"
```
After completing these steps, the application will be able to start successfully. The `spring.cloud.vault.fail-fast: true` property ensures that the application will not start if it cannot connect to Vault and read the secrets.

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
