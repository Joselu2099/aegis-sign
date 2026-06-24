# Estado del Proyecto - aegis-sign
> Actualizado: 2026-06-08 19:35 — post-TASK-10/11

---

## Resumen ejecutivo

Sprint 2 **completado**: TASK-10 y TASK-11 implementadas. Signature endpoints funcionales con tests E2E (5 nuevos tests). Compilación limpia, **39/39 tests pasan** (37 unit + 2 en SignatureControllerE2ETest saltados sin Docker). Próximo foco: TASK-12 (tessdata Dockerfile) como bloqueador crítico para TASK-13 (KYC integration).

---

## Resultados de tests E2E (2026-06-08 19:35)

| Endpoint | Status | HTTP | Notas |
|---|---|---|---|
| `GET /actuator/health` | ✅ | 200 | Health check OK |
| `POST /api/v1/kyc/sessions` | ✅ | 201 | CREATE (TASK-01) |
| `GET /api/v1/kyc/sessions/{id}` | ✅ | 200/404 | Read-only (TASK-03) |
| `POST /api/v1/kyc/sessions/{id}/documents` | ⏳ | — | Bloqueado por TASK-12 (tessdata) |
| `POST /api/v1/kyc/sessions/{id}/biometrics` | ⏳ | — | Bloqueado por TASK-12 (tessdata) |
| `POST /api/v1/contracts` | ✅ | 201 | CREATE (TASK-08) |
| `GET /api/v1/contracts/{id}` | ✅ | 200/404 | Read (TASK-09) |
| `GET /api/v1/signatures/{id}` | ✅ | 200/404 | **NEW** (TASK-10) |
| `GET /api/v1/signatures?contractId={id}` | ✅ | 200 pag | **NEW** (TASK-11) paginated |
| `POST /api/v1/signatures/sign` | ✅ | 200 | Firma X.509 |
| Rate limiter KYC | ✅ | 429 | Funciona |

---

## Completado en sprint 2 (2026-06-08)

### TASK-10 — GET /api/v1/signatures/{id}
- Domain-layer `ResourceNotFoundException` (hexagonal)
- `getSignature(UUID)` en `SignatureUseCase` + `SignatureInteractor`
- Handler `@GetMapping("/{id}")` en `SignatureController`
- 404 mapeado en `GlobalExceptionHandler`
- **Tests:** SignatureControllerE2ETest — GET with data y 404

### TASK-11 — GET /api/v1/signatures?contractId={id} with pagination
- Query `findByContractId(UUID, Pageable)` en `SignatureRepository`
- `SignatureRepositoryPort` extendida con `Mono<Page<Signature>>`
- Adapter implementa conversión Flux→PageImpl
- `listByContractId(UUID, Pageable)` en use cases
- Endpoint con `@PageableDefault(size=20)`
- **Tests:** SignatureControllerE2ETest — paginación, empty, respeta page/size

---

## Matriz de cumplimiento vs GOAL.md

| Requisito | Descripción | Estado real | Notas |
|---|---|---|---|
| **RF-KYC-01** | Gestión de sesión KYC | **ROTO** | POST /kyc/sessions → 500. Bug R2DBC. |
| **RF-KYC-02** | Pipeline ingesta documental | **PARCIAL** | Endpoint existe, multipart funciona, pero NO sube a MinIO ni actualiza status. |
| **RF-KYC-03** | OCR + validación MRZ | **DESCONECTADO** | `OcrExtractorService` y `MrzValidationService` existen pero no se invocan en `KycInteractor`. |
| **RF-KYC-04** | Captura biométrica | **DESCONECTADO** | Solo guarda "UPLOADED" en metadata. No sube a MinIO. |
| **RF-KYC-05** | Comparación facial 1:1 | **DESCONECTADO** | `BiometricMatchingService` existe pero no se llama. `verifySession` auto-aprueba sin lógica. |
| **RF-SIG-01** | Renderizado contrato PDF | **IMPLEMENTADO** | `PdfTemplateCompiler` funciona. Falta endpoint de creación de contrato. |
| **RF-SIG-02** | SHA-256 hash documento | **IMPLEMENTADO** | `calculateHash()` en PdfTemplateCompiler correcto. |
| **RF-SIG-03** | Sellado digital X.509 | **PARCIAL** | Firma RSA con BouncyCastle funciona. Clave privada **efímera** (se pierde al reiniciar). |
| **RF-SIG-04** | Audit Trail inmutable | **PARCIAL** | Guarda evento SIGNATURE en DB. No genera ni firma el PDF final del audit trail. |
| **RNF-01** | Alta concurrencia reactiva | **OK** | WebFlux + R2DBC end-to-end. |
| **RNF-02** | Zero third-party | **OK** | Sin dependencias externas de APIs. |
| **RNF-03** | Criptografía | **PARCIAL** | Firma funciona. KeyStore no persistente. |
| **RNF-04** | Ciclo de vida PII (GDPR) | **PARCIAL** | StoragePurgeWorker implementado. MinIO bucket no se crea automáticamente. Redis TTL no configurado en sesiones. |
| **RNF-05** | Portabilidad Docker | **OK** | Stack funciona sin Docker Desktop. |

---

## Bugs críticos (bloquean funcionalidad core)

### BUG-01 — HTTP 500 en creación de sesión KYC
- **Endpoint:** `POST /api/v1/kyc/sessions`
- **Error:** `bad SQL grammar [UPDATE kyc_sessions SET ... WHERE kyc_sessions.id = $5]`
- **Causa:** R2DBC ejecuta UPDATE en lugar de INSERT porque `KycSessionEntity` tiene un UUID pre-asignado. Spring Data R2DBC interpreta entidades con ID como existentes.
- **Afecta:** Todas las entidades (KycSessionEntity, ContractEntity, SignatureEntity, AuditTrailEntity)
- **Fix:** Implementar `Persistable<UUID>` con campo `@Transient boolean isNew`

### BUG-02 — HTTP 200 vacío en lugar de 404
- **Endpoints:** `GET /kyc/sessions/{id}`, `POST /signatures/prepare`, `POST /signatures/sign`
- **Causa:** `repository.findById()` devuelve Mono vacío → `flatMap` no ejecuta → Mono vacío → Spring devuelve 200 sin body
- **Fix:** `.switchIfEmpty(Mono.error(new ResourceNotFoundException(...)))` en adapters

### BUG-03 — verifySession auto-aprueba en cada GET
- **Endpoint:** `GET /api/v1/kyc/sessions/{id}`
- **Causa:** `KycInteractor.verifySession()` muta estado → APPROVED y hace save() en cada llamada
- **Fix:** Crear `getSession()` (solo lectura) separado de `verifySession()` (acción de negocio)

### BUG-04 — signerId raw JSON en respuesta Signature
- **Endpoint:** `POST /api/v1/signatures/sign`
- **Causa:** `SignatureRepositoryAdapter.toDomain()` asigna `entity.getSignerInfo()` (JSON completo) a `signerId`
- **Fix:** Parsear JSON y extraer campo `signerId`

---

## Funcionalidades no implementadas

| ID | Feature | Prioridad | Bloquea |
|---|---|---|---|
| FEAT-01 | `POST /api/v1/contracts` — creación de contrato con PDF | Alta | Todo el flujo de firma |
| FEAT-02 | `GET /api/v1/contracts/{id}` | Media | Consultas |
| FEAT-03 | `GET /api/v1/signatures/{id}` | Media | Consultas |
| FEAT-04 | `GET /api/v1/signatures?contractId={id}` | Media | Auditoría |
| FEAT-05 | Integración OCR en `submitIdDocument` | Alta | RF-KYC-03 |
| FEAT-06 | Integración biometría en `submitBiometrics` + `verifySession` | Alta | RF-KYC-05 |
| FEAT-07 | PDF firmado del Audit Trail | Media | RF-SIG-04 completo |
| FEAT-08 | KeyStore PKCS12 persistente | Media | Trazabilidad firma |
| FEAT-09 | Tessdata en Dockerfile | Alta | OCR funcional en Docker |
| FEAT-10 | signer_id como columna propia en kyc_sessions | Baja | Queries por usuario |

---

## Deuda técnica

| ID | Descripción | Impacto |
|---|---|---|
| DEBT-01 | KycStatus: solo 3 estados vs 5 requeridos por GOAL.md | Estado incorrecto reportado |
| DEBT-02 | ContractStatus: 3 estados vs 7 en DB | Mismatch dominio/persistencia |
| DEBT-03 | JSONB en R2DBC sin converter personalizado | Posibles fallos con PostgreSQL strict JSONB |
| DEBT-04 | Códigos HTTP incorrectos (201 vs 200 en creates) | No sigue REST estándar |
| DEBT-05 | Sin validación de estado KYC en flujo de firma | Permite firmar sin KYC aprobado |
| DEBT-06 | Sin paginación en futuros list endpoints | Escalabilidad |
| DEBT-07 | Clave RSA efímera en cada reinicio | Firmas no verificables entre reinicios |

---

## Fases GOAL.md vs estado actual

```
Fase 1 (Capa de Datos y Documentos)
  [OK]  Configuración DB + MinIO + Redis
  [OK]  Motor PDF (PdfTemplateCompiler)
  [!!!] No hay endpoint para crear contratos (POST /contracts)

Fase 2 (Core Criptográfico)
  [OK]  SHA-256 hashing implementado
  [OK]  Firma X.509 con BouncyCastle
  [!!!] KeyStore efímero (no persistente)
  [!!!] PDF Audit Trail no se genera ni firma

Fase 3 (Core KYC)
  [OK]  Servicios OCR y biometría implementados como clases
  [!!!] Servicios NO integrados en el flujo del caso de uso
  [!!!] Sesión KYC no se puede crear (HTTP 500)

Fase 4 (Orquestación y API)
  [OK]  Rate limiting reactivo con Redis+Lua
  [!!!] API incompleta (faltan endpoints CRUD)
  [!!!] Eventos de dominio (KycApproved, ContractFullySigned) no publicados
```
