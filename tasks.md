# aegis-sign — Task Backlog
> Generado: 2026-06-08 | Actualizado: 2026-06-08 post-merge sprint-2 | Base: project_status.md + análisis E2E

## Estado del proyecto (2026-06-08 post-merge)
- **Compilación:** ✅ JDK 21 (Microsoft OpenJDK 21.0.11)
- **Tests:** ✅ 37/37 pasan · 0 fallos · Tests de integración (Docker) se saltan correctamente sin crash
- **Merge conflicts resueltos:** `GlobalExceptionHandler.java` (imports KYC + Template)
- **Bugs post-merge corregidos:** `KycStatus.PENDING→PENDING_DOCUMENTS`, `ContractEntity.signerIds` faltante, `KycInteractor` no implementaba `getSession()`, `SignatureServiceAdapter` no cerraba `InputStream` del keystore

Leyenda prioridad: 🔴 CRÍTICA · 🟠 ALTA · 🟡 MEDIA · 🟢 BAJA  
Modelos: **Opus** = claude-opus-4-8 · **Sonnet** = claude-sonnet-4-6 · **Haiku** = claude-haiku-4-5

---

## BLOQUE 0 — Desbloqueadores (sin estos nada funciona)

### TASK-01 · ✅ COMPLETADA ~~🔴 CRÍTICA~~
**Fix BUG-01: R2DBC ejecuta UPDATE en lugar de INSERT en todas las entidades**

- **Causa raíz:** `KycSessionEntity`, `ContractEntity`, `SignatureEntity`, `AuditTrailEntity` tienen UUID pre-asignado. Spring Data R2DBC detecta ID presente → interpreta como entidad existente → lanza UPDATE → `bad SQL grammar`.
- **Fix requerido:** Implementar `Persistable<UUID>` en las 4 entidades con campo `@Transient boolean isNew = true` y override de `isNew()`.
- **Archivos afectados:** Las 4 entity classes + sus repositorios R2DBC.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`
- **Criterio de éxito:** `POST /api/v1/kyc/sessions` devuelve 201, registro visible en DB.
- **Bloquea:** TASK-03, TASK-04, TASK-09, TASK-10, TASK-11, TASK-12
- **Completada:** 2026-06-08 — `implements Persistable<UUID>` con `@Transient @Builder.Default boolean isNew = true` + métodos `getId()`/`isNew()` explícitos en 4 entidades. `KycRepositoryAdapter.save()` usa `existsById()` para diferenciar INSERT/UPDATE.

---

### TASK-02 · ✅ COMPLETADA ~~🔴 CRÍTICA~~
**Fix BUG-02: HTTP 200 vacío en lugar de 404 cuando recurso no existe**

- **Causa raíz:** `repository.findById()` devuelve Mono vacío → `flatMap` no ejecuta → Spring emite 200 sin body. Afecta `GET /kyc/sessions/{id}`, `POST /signatures/prepare`, `POST /signatures/sign`.
- **Fix requerido:** Añadir `.switchIfEmpty(Mono.error(new ResourceNotFoundException(...)))` en los adapters de repositorio correspondientes. Verificar que `ResourceNotFoundException` esté mapeada a 404 en el `GlobalExceptionHandler`.
- **Archivos afectados:** `KycRepositoryAdapter`, `SignatureRepositoryAdapter`, `GlobalExceptionHandler`.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`
- **Criterio de éxito:** Los 3 endpoints devuelven 404 con body de error estructurado cuando el ID no existe.
- **Bloquea:** TASK-05, TASK-11
- **Completada:** 2026-06-08 — `.switchIfEmpty(Mono.error(new ResourceNotFoundException(...)))` en `findById()` de `KycRepositoryAdapter`, `ContractRepositoryAdapter` y `SignatureRepositoryAdapter`.

---

### TASK-03 · ✅ COMPLETADA ~~🔴 CRÍTICA~~
**Fix BUG-03: `verifySession` auto-aprueba y muta estado en cada GET**

- **Causa raíz:** `KycInteractor.verifySession()` muta estado a APPROVED y hace `save()`. Este método es llamado por el handler del `GET /kyc/sessions/{id}`.
- **Fix requerido:** Crear método `getSession(UUID id): Mono<KycSession>` de solo lectura en `KycInteractor`. Separar `verifySession()` como acción de negocio explícita (POST o acción interna al completar biometría). Actualizar el router/handler para que GET llame a `getSession()`.
- **Archivos afectados:** `KycInteractor`, `KycSessionHandler` (o router equivalente), puerto de entrada KYC.
- **Modelo:** **Opus** — decisión de diseño en la arquitectura hexagonal, separar casos de uso.
- **Skills:** `java-development-guide`, `writing-plans`, `realizar-correctivo`, `verification-before-completion`
- **Criterio de éxito:** `GET /kyc/sessions/{id}` devuelve estado real sin mutarlo. `verifySession` solo se invoca explícitamente.
- **Depende de:** TASK-01 (para poder crear sesiones y probar)
- **Completada:** 2026-06-08 — `getSession()` añadido a `KycUseCase` + `KycInteractor` (solo `findById`). `KycController.getSession()` llama `getSession()`. `verifySession()` conservado para TASK-14.

---

### TASK-04 · ✅ COMPLETADA ~~🔴 CRÍTICA~~
**Fix BUG-04: `signerId` contiene JSON raw en respuesta de Signature**

- **Causa raíz:** `SignatureRepositoryAdapter.toDomain()` asigna `entity.getSignerInfo()` (JSONB completo) al campo `signerId` del dominio.
- **Fix requerido:** Parsear el JSONB en `toDomain()` y extraer el campo `signerId`. Usar `ObjectMapper` o parser manual. Añadir converter JSONB si aún no existe (relacionado con DEBT-03).
- **Archivos afectados:** `SignatureRepositoryAdapter`, posiblemente `SignatureEntity`.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `realizar-correctivo`
- **Criterio de éxito:** `POST /api/v1/signatures/sign` devuelve `signerId` como string, no como objeto JSON.
- **Completada:** 2026-06-08 — `ObjectMapper` inyectado en `SignatureRepositoryAdapter`. Método `extractSignerId()` parsea `{"signerId":"<valor>"}` y extrae el campo. `toDomain()` usa `extractSignerId()` en lugar de asignación directa.

---

## BLOQUE 1 — Dominio: estados y modelos

### TASK-05 · ✅ COMPLETADA ~~🟠 ALTA~~
**DEBT-01: Expandir `KycStatus` de 3 a 5 estados según GOAL.md**

- **Estados faltantes:** Identificar en GOAL.md los 5 estados requeridos vs los 3 actuales (`PENDING`, `APPROVED`, `REJECTED`). Añadir los estados intermedios (ej. `DOCUMENT_SUBMITTED`, `BIOMETRICS_SUBMITTED` o equivalentes).
- **Impacto:** Sin estados completos, el flujo KYC no puede reportar progreso real.
- **Archivos afectados:** `KycStatus` enum, migraciones Flyway/Liquibase, `KycInteractor`, queries R2DBC.
- **Modelo:** **Opus** — afecta lógica de negocio y transiciones de estado.
- **Skills:** `java-development-guide`, `writing-plans`
- **Criterio de éxito:** Enum alineado con GOAL.md, migración de DB incluida, tests de transición.
- **Depende de:** TASK-01
- **Completada:** 2026-06-08 — Enum expandido a `PENDING_DOCUMENTS`, `PROCESSING`, `MANUAL_REVIEW`, `APPROVED`, `REJECTED`. `KycInteractor.submitIdDocument/Biometrics()` ahora transicionan a `PROCESSING`. `KycRepositoryAdapter` con mapping 5↔5. `V2__update_kyc_status.sql` migra `CREATED`→`PENDING_DOCUMENTS` y actualiza CHECK. 3 test files actualizados.

---

### TASK-06 · ✅ COMPLETADA ~~🟠 ALTA~~
**DEBT-02: Expandir `ContractStatus` de 3 a 7 estados según schema DB**

- **Causa:** Mismatch entre enum de dominio (3 estados) y columna DB (7 posibles). Riesgo de `IllegalArgumentException` al deserializar.
- **Fix requerido:** Revisar schema DB, identificar los 7 estados, actualizar enum `ContractStatus`, actualizar lógica de transición en `ContractInteractor`.
- **Archivos afectados:** `ContractStatus` enum, `ContractInteractor`, `ContractRepositoryAdapter`.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `realizar-correctivo`
- **Criterio de éxito:** No hay mismatch enum/DB. Todos los estados representados.
- **Completada:** 2026-06-08 — Enum expandido a `DRAFT, PREPARED, PENDING_SIGNATURE, SIGNED, CANCELLED, EXPIRED, REVOKED`. Sin migración DB (V1 ya tenía los 7). Sin cambios en adapter ni tests.

---

### TASK-07 · ✅ COMPLETADA ~~🟠 ALTA~~
**DEBT-03: Implementar converter JSONB personalizado para R2DBC**

- **Causa:** R2DBC no convierte JSONB de PostgreSQL automáticamente. Campos como `metadata`, `signerInfo`, `details` pueden fallar con PostgreSQL strict JSONB.
- **Fix requerido:** Implementar `io.r2dbc.postgresql.codec.Json` o converter custom con `ObjectMapper`. Registrar en `R2dbcCustomConversions`.
- **Archivos afectados:** Configuración R2DBC, entidades con campos JSONB.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`
- **Criterio de éxito:** JSONB se serializa/deserializa correctamente sin errores en todos los endpoints.
- **Depende de:** TASK-01
- **Completada:** 2026-06-08 — Creado `R2dbcConfig.java` con `@ReadingConverter JsonToStringConverter` (`io.r2dbc.postgresql.codec.Json → String`). Registrado en `R2dbcCustomConversions.of(PostgresDialect.INSTANCE, ...)`. Sin escribir converter para no capturar columnas VARCHAR.

---

## BLOQUE 2 — API: endpoints faltantes

### TASK-08 · ✅ COMPLETADA ~~🟠 ALTA~~
**FEAT-01: Implementar `POST /api/v1/contracts` — creación de contrato con PDF**

- **Contexto:** `PdfTemplateCompiler` ya funciona. Falta el endpoint que lo orqueste: recibir datos del contrato, compilar PDF, calcular SHA-256, guardar `ContractEntity`, subir PDF a MinIO.
- **Flujo completo:**
  1. Validar request body (signerIds, templateId, datos del contrato)
  2. Llamar `PdfTemplateCompiler.compile()`
  3. Llamar `DocumentHashingService.calculateHash()`
  4. Subir PDF compilado a MinIO
  5. Guardar `ContractEntity` con status `DRAFT`
  6. Devolver 201 con contract ID y hash
- **Archivos a crear/modificar:** Handler, Router, caso de uso en `ContractInteractor`, puerto de entrada.
- **Modelo:** **Opus** — orquestación multi-servicio, arquitectura hexagonal.
- **Skills:** `java-development-guide`, `writing-plans`, `test-driven-development`, `verification-before-completion`
- **Criterio de éxito:** `POST /api/v1/contracts` devuelve 201 con contractId. PDF existe en MinIO. Hash SHA-256 correcto en DB.
- **Depende de:** TASK-01, TASK-06, TASK-07
- **Bloquea:** Todo el flujo de firma (TASK-12, TASK-13)
- **Completada:** 2026-06-08 — Endpoint funcional con orquestación reactiva (`Schedulers.boundedElastic` para ops blocking). Status inicial `PREPARED` (PDF ya compilado y subido). signerIds **persistidos** en columna `signer_ids JSONB` (migración V3). `TemplateNotFoundException` en `domain.exception` con handler 404. Plantilla `templates/sample-contract.json` para test inmediato.
- **Ficheros nuevos:** `ContractUseCase`, `ContractInteractor`, `ContractController`, `TemplateResolver`, `TemplateNotFoundException`, `V3__add_contract_signer_ids.sql`, `templates/sample-contract.json`. **Modificados:** `Contract` (domain), `ContractEntity`, `ContractRepositoryAdapter`, `GlobalExceptionHandler`.

---

### TASK-09 · ✅ COMPLETADA ~~🟡 MEDIA~~
**FEAT-02: Implementar `GET /api/v1/contracts/{id}`**

- **Fix:** Añadir handler de lectura en `ContractInteractor.getContract()`. Incluir `.switchIfEmpty(404)`.
- **Modelo:** **Haiku** — implementación estándar CRUD.
- **Skills:** `java-development-guide`, `realizar-correctivo`
- **Criterio de éxito:** 200 con datos del contrato, 404 si no existe.
- **Depende de:** TASK-08
- **Completada:** 2026-06-08 — `ContractUseCase.getContract(UUID id)` + `ContractInteractor` implementación + handler `@GetMapping("/{id}")` en `ContractController`.

---

### TASK-10 · 🟡 MEDIA
**FEAT-03: Implementar `GET /api/v1/signatures/{id}`**

- **Fix:** Añadir caso de uso `getSignature()` en `SignatureInteractor` + handler `@GetMapping("/{id}")` en `SignatureController`. Incluir `.switchIfEmpty(404)`.
- **Estado actual:** `SignatureController` solo tiene `/prepare` y `/sign`. Falta GET por ID.
- **Modelo:** **Haiku**
- **Skills:** `java-development-guide`, `realizar-correctivo`
- **Criterio de éxito:** 200 con datos de firma, 404 si no existe.
- **Depende de:** TASK-01, TASK-02

---

### TASK-11 · 🟡 MEDIA
**FEAT-04: Implementar `GET /api/v1/signatures?contractId={id}`**

- **Fix:** Query en `SignatureRepository` por `contractId`. Handler con paginación básica (ver DEBT-06).
- **Estado actual:** No implementado.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`
- **Criterio de éxito:** Devuelve lista de firmas para un contrato. 200 con array vacío si no hay firmas.
- **Depende de:** TASK-01, TASK-08

---

## BLOQUE 3 — Integración KYC (servicios existentes desconectados)

### TASK-12 · 🟠 ALTA
**FEAT-09: Añadir tessdata al Dockerfile para OCR funcional en Docker**

- **Contexto:** `OcrExtractorService` usa Tesseract. Sin tessdata en la imagen Docker, OCR falla en runtime.
- **Fix requerido:** Añadir en `Dockerfile`: instalación de `tesseract-ocr` + `tesseract-ocr-spa` (u otros idiomas necesarios) + configuración de `TESSDATA_PREFIX`.
- **Archivos afectados:** `Dockerfile`, posiblemente `docker-compose.yml`.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `verification-before-completion`
- **Criterio de éxito:** OCR funciona dentro del container Docker sin errores de tessdata.
- **Bloquea:** TASK-13

---

### TASK-13 · ✅ PARCIALMENTE COMPLETADA — 🟠 ALTA PENDIENTE TASK-12
**FEAT-05: Integrar `OcrExtractorService` y `MrzValidationService` en `submitIdDocument`**

- **Estado actual:** OCR+MRZ integrados en `KycInteractor.submitIdDocument()` (lógica TD1/TD2/TD3 completa). **Falta:** subida a MinIO del documento antes de OCR (requiere TASK-12 para tessdata en Docker).
- **Completado en sprint-2:**
  - `OcrExtractorService.extractData()` invocado sobre `content`
  - `MrzValidationService.validateChecksum()` con validaciones TD1/TD2/TD3
  - Transición a `KycStatus.MRZ_FAILED` si validación falla
  - `BiometricValidationService` integrado en `submitBiometrics` (calidad, detección facial, liveness mock)
  - Transición a `KycStatus.BIOMETRIC_FAILED` si validación biométrica falla
- **Pendiente:** subir documento a MinIO + `BiometricMatchingService` (comparación real doc↔biometría) → ver TASK-14
- **Criterio de éxito completo:** `POST /kyc/sessions/{id}/documents` procesa OCR, valida MRZ, sube a MinIO, actualiza estado KYC.
- **Depende de:** TASK-12 (tessdata en Docker para OCR real)

---

### TASK-14 · 🟠 ALTA
**FEAT-06: Integrar `BiometricMatchingService` en `submitBiometrics` y `verifySession`**

- **Contexto:** `BiometricValidationService` (calidad imagen) integrado. Falta `BiometricMatchingService` (comparación facial doc↔foto).
- **Estado actual:** `submitBiometrics` valida calidad, detecta cara mock, liveness mock. **No** compara contra imagen del documento.
- **Flujo pendiente en `submitBiometrics`:**
  1. Recuperar imagen de documento ya subida (de MinIO o metadata)
  2. Llamar `BiometricMatchingService.match(docImage, bioImage)`
  3. Si score < umbral → transición a REJECTED
  4. Si score >= umbral → transición a APPROVED
- **`verifySession`:** actualmente auto-aprueba. Debe llamarse solo internamente tras biometría aprobada.
- **Archivos afectados:** `KycInteractor`, `BiometricMatchingService`, `KycRepositoryAdapter`.
- **Modelo:** **Opus** — lógica de negocio crítica, pipeline reactivo multi-paso.
- **Skills:** `java-development-guide`, `writing-plans`, `test-driven-development`, `verification-before-completion`
- **Criterio de éxito:** `POST /kyc/sessions/{id}/biometrics` ejecuta comparación facial real. Estado KYC refleja resultado.
- **Depende de:** TASK-13

---

## BLOQUE 4 — Firma y criptografía

### TASK-15 · 🟠 ALTA
**DEBT-05: Validar estado KYC aprobado antes de permitir firma**

- **Causa:** `POST /api/v1/signatures/sign` no verifica que el firmante tenga sesión KYC en estado APPROVED. Permite firmar sin identidad verificada.
- **Estado actual:** `SignatureInteractor.signContract()` no consulta KYC.
- **Fix requerido:** En `SignatureInteractor.sign()`, consultar `KycRepository.findBySignerId()`, verificar `status == APPROVED`, lanzar error de negocio si no.
- **Archivos afectados:** `SignatureInteractor`, puerto de salida KYC en dominio de firma.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`
- **Criterio de éxito:** Intento de firma con KYC no aprobado → 422/403 con mensaje claro.
- **Depende de:** TASK-01, TASK-02, TASK-14

---

### TASK-16 · 🟡 MEDIA
**FEAT-08 + DEBT-07: KeyStore PKCS12 persistente entre reinicios**

- **Causa:** Clave RSA generada en memoria → se pierde al reiniciar → firmas anteriores no verificables.
- **Fix requerido:**
  1. Generar KeyStore PKCS12 en primer arranque y serializar a archivo o a MinIO/PostgreSQL (bytea).
  2. En arranques siguientes, cargar KeyStore existente.
  3. Proteger con contraseña configurable vía variable de entorno.
- **Archivos afectados:** `KeyStoreService` (o equivalente), configuración Spring, `docker-compose.yml` (volumen o env var).
- **Modelo:** **Opus** — seguridad criptográfica, decisión de almacenamiento.
- **Skills:** `java-development-guide`, `writing-plans`, `verification-before-completion`
- **Criterio de éxito:** Reiniciar el contenedor no invalida firmas previas. KeyStore cargado correctamente en arranque.
- **Depende de:** TASK-01

---

### TASK-17 · 🟡 MEDIA
**FEAT-07: Generar y firmar PDF del Audit Trail**

- **Contexto:** Se guarda evento SIGNATURE en DB pero no se genera ni firma el PDF del audit trail.
- **Flujo requerido:**
  1. Al completar firma → recopilar todos los eventos de `AuditTrailEntity` para ese contrato
  2. Renderizar PDF de audit trail (nuevo template o `PdfTemplateCompiler`)
  3. Firmar PDF con clave X.509 (reusar `DigitalSealingService`)
  4. Subir PDF firmado a MinIO
  5. Guardar referencia en DB
- **Archivos afectados:** `SignatureInteractor`, `AuditTrailInteractor` (nuevo o existente), `PdfTemplateCompiler`.
- **Modelo:** **Opus** — orquestación multi-servicio post-firma.
- **Skills:** `java-development-guide`, `writing-plans`, `test-driven-development`
- **Criterio de éxito:** Tras firma, existe en MinIO un PDF de audit trail firmado digitalmente.
- **Depende de:** TASK-08, TASK-16

---

## BLOQUE 5 — Infraestructura y configuración

### TASK-18 · 🟠 ALTA
**RNF-04: Crear bucket MinIO automáticamente en arranque**

- **Causa:** MinIO bucket no se crea automáticamente. Uploads fallan con bucket-not-found.
- **Fix requerido:** Añadir bean `ApplicationRunner` o `@PostConstruct` que verifique y cree el bucket si no existe. Usar el cliente MinIO ya configurado.
- **Archivos afectados:** Configuración MinIO, nuevo `MinIoBucketInitializer`.
- **Modelo:** **Haiku**
- **Skills:** `java-development-guide`
- **Criterio de éxito:** Al arrancar stack desde cero, bucket existe antes de primer upload.

---

### TASK-19 · 🟡 MEDIA
**RNF-04: Configurar Redis TTL en sesiones KYC**

- **Causa:** Redis TTL no configurado en sesiones → datos PII persisten indefinidamente → incumple GDPR.
- **Fix requerido:** Al guardar sesión KYC en Redis (rate limiter o caché), configurar TTL apropiado (ej. 24h). Verificar `StoragePurgeWorker` invoca limpieza en Redis además de MinIO.
- **Archivos afectados:** `StoragePurgeWorker`, configuración Redis, posibles repositorios Redis.
- **Modelo:** **Sonnet**
- **Skills:** `java-development-guide`, `verification-before-completion`
- **Criterio de éxito:** Sesiones KYC expiran en Redis tras TTL configurado. `StoragePurgeWorker` limpia MinIO y Redis.

---

## BLOQUE 6 — Correcciones REST y deuda técnica menor

### TASK-20 · 🟡 MEDIA — PARCIAL
**DEBT-04: Corregir códigos HTTP en endpoints de creación (200 → 201)**

- **Estado actual:**
  - `POST /contracts` ✅ ya tiene `@ResponseStatus(HttpStatus.CREATED)`
  - `POST /kyc/sessions` ❌ devuelve 200 (falta `@ResponseStatus(CREATED)` en `KycController.createSession`)
  - `POST /signatures/sign` — semánticamente es acción, 200 aceptable
- **Fix pendiente:** Solo `KycController.createSession` necesita `@ResponseStatus(HttpStatus.CREATED)`.
- **Modelo:** **Haiku**
- **Skills:** `realizar-correctivo`
- **Criterio de éxito:** `POST /kyc/sessions` devuelve 201.
- **Depende de:** TASK-01

---

### TASK-21 · 🟢 BAJA
**FEAT-10: Añadir columna `signer_id` propia en `kyc_sessions`**

- **Causa:** `signerId` actualmente en JSONB metadata. Dificulta queries por usuario.
- **Fix requerido:** Migración Flyway: añadir columna `signer_id VARCHAR/UUID`. Actualizar `KycSessionEntity`. Backfill si hay datos.
- **Modelo:** **Haiku**
- **Skills:** `java-development-guide`
- **Criterio de éxito:** Columna existe, queries por `signer_id` funcionan sin parsear JSONB.
- **Depende de:** TASK-01

---

### TASK-22 · 🟢 BAJA
**DEBT-06: Añadir paginación a endpoints de listado**

- **Afecta:** `GET /signatures?contractId=` (TASK-11) y futuros endpoints de lista.
- **Fix:** Usar `PageRequest` + `Pageable` en queries R2DBC. Devolver wrapper con `content`, `totalElements`, `page`, `size`.
- **Modelo:** **Haiku**
- **Skills:** `java-development-guide`
- **Criterio de éxito:** Endpoints de lista aceptan `?page=0&size=20`. Response incluye metadata de paginación.
- **Depende de:** TASK-11

---

## BLOQUE 7 — Calidad y verificación

### TASK-23 · 🟠 ALTA
**Tests E2E: Cubrir flujo completo KYC → Contrato → Firma**

- **Scope:** Tras completar BLOQUEs 0-4, ejecutar suite E2E completa:
  1. `POST /kyc/sessions` → 201
  2. `POST /kyc/sessions/{id}/documents` → OCR + MRZ válida
  3. `POST /kyc/sessions/{id}/biometrics` → match facial → APPROVED
  4. `POST /contracts` → PDF generado, hash correcto
  5. `POST /signatures/sign` → firma X.509, audit trail
  6. `GET /signatures/{id}` → datos correctos
- **Modelo:** **Opus** — diseño de suite E2E, aserciones de negocio.
- **Skills:** `test-driven-development`, `verification-before-completion`, `java-development-guide`
- **Criterio de éxito:** Todos los pasos del flujo pasan. Zero HTTP 500. Status codes correctos.
- **Depende de:** TASK-01 a TASK-17 completados

---

### TASK-24 · 🟡 MEDIA
**Code review de seguridad: inputs, GDPR, criptografía**

- **Scope:** Revisar validación de inputs en todos los endpoints, manejo de PII, uso correcto de BouncyCastle, no exponer stack traces en respuestas de error.
- **Modelo:** **Opus**
- **Skills:** `security-review`, `code-review`, `java-development-guide`
- **Criterio de éxito:** No hay vulnerabilidades OWASP Top 10 obvias. PII no se loguea. Errores no exponen internals.
- **Depende de:** TASK-23

---

## Orden de ejecución recomendado (estado actualizado 2026-06-08)

```
✅ Sprint 1 — Desbloqueadores COMPLETOS
  TASK-01 ✅ → TASK-02 ✅ → TASK-03 ✅ → TASK-04 ✅

✅ Sprint 2 — Dominio y API base COMPLETOS
  TASK-05 ✅ → TASK-06 ✅ → TASK-07 ✅
  TASK-08 ✅ → TASK-09 ✅
  TASK-18 (independiente, pendiente)

🔄 Sprint 3 — En curso
  TASK-10 (pendiente) → TASK-11 (pendiente)
  TASK-12 (pendiente) → TASK-14 (parcial: calidad OK, matching pendiente)
  TASK-15 (pendiente, depende TASK-14)

Sprint 4 — Firma completa
  TASK-16 (pendiente) → TASK-17 (pendiente)
  TASK-19 (independiente, pendiente)

Sprint 5 — Pulido
  TASK-20 (parcial) → TASK-21 → TASK-22
  TASK-23 → TASK-24
```

---

## Resumen por modelo

| Modelo | Tasks pendientes |
|--------|-------|
| **Opus** | TASK-14, TASK-16, TASK-17, TASK-23, TASK-24 |
| **Sonnet** | TASK-11, TASK-12, TASK-15, TASK-19 |
| **Haiku** | TASK-10, TASK-18, TASK-20 (parcial), TASK-21, TASK-22 |

## Resumen por prioridad

| Prioridad | Count | Tasks |
|-----------|-------|-------|
| 🔴 CRÍTICA | 4 | TASK-01..04 ✅ todas completadas |
| 🟠 ALTA | pendientes | TASK-12, TASK-14, TASK-15, TASK-18, TASK-23 |
| 🟡 MEDIA | pendientes | TASK-10, TASK-11, TASK-16, TASK-17, TASK-19, TASK-20, TASK-24 |
| 🟢 BAJA | 2 | TASK-21, 22 |
