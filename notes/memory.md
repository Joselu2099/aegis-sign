# Memoria del Proyecto, Backlog y Registro de Aprendizaje

## Contexto de Sesión Actual y Objetivo Activo

- **Estado**: Mantenimiento activo / consolidación de documentación técnica exhaustiva.
- **Objetivo actual**: Migrar y actualizar la documentación previa (`docs/architecture.md`, `docs/business-logic.md`, `docs/database.md`, `docs/memory.md`) a la estructura estandarizada `context/` y `notes/`, verificando cada afirmación contra el código real (se detectaron varias divergencias respecto a la documentación anterior — ver Base de Conocimiento).
- **Entorno**: macOS, Java 21, Maven, Spring Boot 3.2.5, PostgreSQL, Redis, MinIO, Tesseract OCR nativo, HashiCorp Vault (config en tiempo de arranque).

## Base de Conocimiento (Lo que se ha aprendido)

- **Propósito del proyecto**: Microservicio reactivo de identidad y firma electrónica avanzada (FEA): KYC (OCR + MRZ + biometría) y firma digital de contratos con X.509, autoalojado (sin dependencias de APIs de IA en la nube).
- **Arquitectura**: Hexagonal (puertos y adaptadores) con programación reactiva integral (Spring WebFlux + R2DBC). 4 entidades de persistencia (`infrastructure/adapter/db/entity/`) ↔ 5 modelos de dominio (`domain/model/`); el modelo de dominio adicional `MatchResult` es un objeto de transferencia transitorio del proceso de matching biométrico, no tiene tabla propia.
- **Inconsistencia activa CHECK constraint vs. código** (hallazgo importante): `KycRepositoryAdapter.mapStatusToDb` mapea `KycStatus.MRZ_FAILED` y `KycStatus.BIOMETRIC_FAILED` al literal de BD `"FAILED"`, pero la migración `V2__update_kyc_status.sql` **eliminó** `'FAILED'` del `ck_kyc_status` CHECK constraint (solo permite `PENDING_DOCUMENTS, PROCESSING, MANUAL_REVIEW, APPROVED, REJECTED`). Cualquier sesión KYC que falle validación MRZ o biométrica fallará al persistirse con una violación de constraint en PostgreSQL real (no se manifiesta en tests con mocks de repositorio). Requiere una migración `V4` que añada `'FAILED'` de nuevo al constraint, o cambiar el mapeo para usar `REJECTED`.
- **`PersistenceSerializationException` sin handler dedicado**: existe la excepción (creada para fallar rápido en (de)serialización JSON, ver historial de commits `fix: stop swallowing JSON (de)serialization failures`), pero `GlobalExceptionHandler` no tiene un `@ExceptionHandler` específico para ella — cae en el handler genérico de `Exception.class` y responde HTTP 500 genérico sin código de error específico. Sería razonable añadir un handler dedicado (ej. `INTERNAL_SERVER_ERROR` con `errorCode = "PERSISTENCE_SERIALIZATION_ERROR"`) para diferenciarla de errores de programación genéricos.
- **`audit_trails.contract_id` sin UNIQUE constraint**: el modelo de negocio asume relación 1:1 entre `Contract` y `AuditTrail`, pero la base de datos solo tiene un índice no-único (`idx_audit_trails_contract_id`). Nada impide insertar múltiples audit trails para el mismo contrato (por ejemplo, si `signContract` se invoca dos veces erróneamente antes de que `markAsSigned()` bloquee la segunda firma — y de hecho `SignatureInteractor.signContract` no usa `Contract.markAsSigned()`, solo hace `contract.setStatus(SIGNED)` directamente, evitando el chequeo `IllegalStateException` de doble firma definido en el propio modelo de dominio).
- **`SignatureEntity.signerInfo` construido por concatenación manual de String**: `SignatureRepositoryAdapter.toEntity` construye el JSON `{"signerId":"..."}")` con concatenación de strings en lugar de `ObjectMapper.writeValueAsString(...)`. Si `signerId` contiene comillas dobles o caracteres de escape, el JSON resultante queda corrupto y `extractSignerId` (que usa una regex, no un parser JSON real) puede devolver datos incorrectos silenciosamente.
- **`SoftwareKeyStoreEncryptionAdapter` no usa un KeyStore real**: a pesar de vivir en el paquete `infrastructure/adapter/keystore/`, esta clase genera una clave AES-256 efímera en memoria con `KeyGenerator` en cada arranque de la aplicación — no carga ni persiste la clave en `keystore.p12` ni en ningún almacén externo. Esto significa que cualquier `certificateThumbprint` cifrado antes de un reinicio del servicio queda indescifrable después del reinicio (la clave se pierde). El backlog histórico (`docs/memory.md` original) ya señalaba "Implementar gestión de KeyStore persistente (.p12)" como tarea pendiente; sigue sin resolverse.
- **Verificación KYC simulada**: `KycInteractor.verifySession` no contiene lógica real de revisión — aprueba automáticamente cualquier sesión existente (`status = APPROVED`) sin validar que `mrzValid`/`biometricValid` sean `true`. Además, este método del puerto de entrada (`KycUseCase.verifySession`) no tiene un endpoint REST mapeado en `KycController` actualmente, por lo que es código muerto desde la API pública.
- **Comparación biométrica con fallback simulado**: `BiometricMatchingService` solo ejecuta inferencia ONNX real si encuentra un archivo de modelo en `biometrics.model-path` (`src/main/resources/models/face_embedding.onnx` por defecto). Se verificó que el directorio `src/main/resources/models/` **no existe** en el repositorio — por lo tanto, en el entorno actual el matching facial siempre usa `calculateMockSimilarity` (heurística basada en la proporción de tamaños de las dos imágenes + ruido aleatorio), no una comparación facial real. Esto es crítico para entender por qué los resultados de matching no son biométricamente significativos hasta que se provea un modelo ONNX real.
- **Liveness y detección de rostro simulados**: tanto `BiometricValidationService.detectFaceMock`/`calculateLivenessMock` como `BiometricMatchingService.checkLiveness` son heurísticas deterministas/pseudoaleatorias (tamaño de archivo, variación de bytes, hash), no modelos de IA reales. Documentado explícitamente en los propios comentarios del código fuente como "mock".
- **Redis cache helper sin consumidores activos**: `RedisSessionCacheHelper` (get/put/delete genérico con TTL) no tiene ninguna referencia de uso (`@Autowired`/inyección) en los interactores actuales (`KycInteractor`, `ContractInteractor`, `SignatureInteractor`); el único uso confirmado de Redis en producción es el rate limiter (`TokenBucketRateLimiterFilter`).
- **Flyway sobre JDBC, no R2DBC**: aunque la app usa R2DBC en tiempo de ejecución, las migraciones de esquema se ejecutan mediante una conexión JDBC separada (`spring.flyway.url`), patrón estándar dado que Flyway no soporta drivers reactivos directamente.
- **Convención de excepciones unificada (de historial de commits)**: tras varios commits de refactor (`refactor: unify not-found convention so adapters stay dumb, interactors decide`), la decisión de arquitectura vigente es que los **adaptadores de infraestructura nunca lanzan `ResourceNotFoundException`**; son los interactores (`ContractInteractor.getContract`, `SignatureInteractor.prepareContractHash/getSignature`, `SignatureInteractor.generateAndSignAuditTrailPdf`) quienes usan `.switchIfEmpty(Mono.error(new ResourceNotFoundException(...)))` sobre el resultado de los puertos de salida. Mantener esta convención en código nuevo.
- **Branching y versionado del repositorio**: nunca se hace commit/push directo a `master`. El flujo observado en el historial usa ramas con prefijo `feature/`, `fix/`, `refactor/` y ramas `release/x.y.z-snapshot` (rama activa actual: `release/0.0.2-snapshot`), fusionadas a `master` mediante merge commits explícitos (`merge: ...`). La versión en `pom.xml` (actualmente `0.0.2-SNAPSHOT`) se incrementa como parte del trabajo de feature/release antes de fusionar.
- **CI/CD**: `ci.yml` corre en cada push a cualquier rama y en PRs contra `master` (instala Tesseract nativo, `mvn clean test`, sube reportes Surefire en fallo, genera resumen de cobertura JaCoCo). `cd.yml` corre solo en push a `master`: si la versión en `pom.xml` no es `-SNAPSHOT` y el tag `vX.Y.Z` no existe, crea el tag, publica un GitHub Release con el JAR adjunto, y construye/publica una imagen Docker en GHCR (`ghcr.io/<repo>:vX.Y.Z` y `:latest`). El auto-tag/release depende de que alguien quite el sufijo `-SNAPSHOT` del `pom.xml` antes de fusionar a `master`.
- **Worktrees + desarrollo dirigido por subagentes**: este repositorio usa `git worktree` (carpeta `.worktrees/`, ignorada en `.gitignore`) para aislar el trabajo de feature branches, combinado con el patrón de revisión en dos etapas (cumplimiento de la especificación, luego calidad de código) antes de fusionar a `release/*`/`master`.

## Backlog del Proyecto (TODOs y Trabajo Futuro)

| Prioridad | Estado | Tarea / Requisito | Contexto |
|-----------|--------|---------------------|----------|
| Alta | TODO | Corregir el CHECK constraint `ck_kyc_status` para incluir `'FAILED'` (o cambiar el mapeo de `MRZ_FAILED`/`BIOMETRIC_FAILED`), evitando fallos de persistencia en producción. | `V2__update_kyc_status.sql`, `KycRepositoryAdapter.mapStatusToDb` |
| Alta | TODO | Implementar gestión de KeyStore persistente para `EncryptionPort` (actualmente la clave AES vive solo en memoria y se pierde en cada reinicio). | `SoftwareKeyStoreEncryptionAdapter` |
| Alta | TODO | Proveer un modelo ONNX real en `biometrics.model-path` y validar el pipeline de matching facial con datos reales (actualmente 100% mock por ausencia de archivo de modelo). | `BiometricMatchingService`, `src/main/resources/models/` |
| Media | TODO | Añadir `@ExceptionHandler` dedicado para `PersistenceSerializationException` en `GlobalExceptionHandler` con código de error específico. | `GlobalExceptionHandler` |
| Media | TODO | Añadir constraint `UNIQUE` sobre `audit_trails.contract_id` para reflejar la relación 1:1 a nivel de base de datos. | Nueva migración Flyway |
| Media | TODO | Sustituir la concatenación manual de strings en `SignatureRepositoryAdapter.toEntity`/`extractSignerId` por serialización/deserialización JSON real con `ObjectMapper`. | `SignatureRepositoryAdapter` |
| Media | TODO | Decidir si `KycInteractor.verifySession` debe implementar lógica de revisión real (validar `mrzValid`/`biometricValid`) y exponer un endpoint REST en `KycController`, o eliminarlo si es código muerto. | `KycInteractor`, `KycUseCase`, `KycController` |
| Media | TODO | Hacer explícito en `SignatureInteractor.signContract` el chequeo de que la `KycSession` esté en estado `APPROVED` antes de firmar (actualmente solo se recupera la sesión, no se valida su estado). | `SignatureInteractor` |
| Baja | TODO | Sustituir o eliminar `RedisSessionCacheHelper` si no tiene consumidores reales, o documentar su uso previsto. | `RedisSessionCacheHelper` |
| Baja | TODO | Soporte de daemon Docker local para tests de integración con Testcontainers (heredado de `docs/requirements.md`). | `pom.xml` perfil `integration-tests` |
| Alta | DONE | Esquema inicial de base de datos (migración V1). | `db/migration/V1__init_schema.sql` |
| Alta | DONE | OCR y validación MRZ (Tess4j + ICAO Doc 9303). | `OcrExtractorService`, `MrzValidationService` |
| Alta | DONE | Sellado digital X.509 (BouncyCastle, `SHA256withRSA`). | `SignatureServiceAdapter` |
| Alta | DONE | Validación y matching biométrico (con fallback simulado documentado). | `BiometricValidationService`, `BiometricMatchingService` |
| Alta | DONE | Worker de purga de almacenamiento temporal (GDPR). | `StoragePurgeWorker` |
| Media | DONE | Generación y firma del PDF de audit trail, exposición vía endpoint REST. | `SignatureInteractor.generateAndSignAuditTrailPdf`, `SignatureController` |
| Media | DONE | Unificación de la convención de excepciones "not found" (adaptadores dumb, interactores decide). | Historial de commits `refactor: unify not-found convention` |
| Media | DONE | Eliminar silenciamiento de errores de (de)serialización JSON en adaptadores Contract/Kyc/AuditTrail. | `PersistenceSerializationException`, historial de commits `fix: stop swallowing JSON...` |

## Registro de Decisiones Significativas

| Fecha | Decisión Técnica | Justificación y Trade-offs | Código Relacionado |
|-------|--------------------|------------------------------|----------------------|
| 2026-05-23 | Arquitectura Hexagonal | Desacoplar la lógica de dominio de la infraestructura (MinIO, Redis, motores OCR). | `domain/`, `application/`, `infrastructure/` |
| 2026-05-23 | Stack reactivo (WebFlux + R2DBC) | Soportar alta concurrencia y streaming de archivos grandes sin bloquear hilos. | Toda la base de código (`Mono`/`Flux`) |
| 2026-05-29 | OCR local (Tess4j) | Garantizar cero dependencia de APIs de OCR en la nube y privacidad de datos sensibles. | `OcrExtractorService` |
| 2026-05-29 | Integración BouncyCastle | Implementar firmas criptográficas reales según estándares FEA. | `SignatureServiceAdapter` |
| 2026-05-29 | ByteBuddy experimental | Habilitar tests con Mockito en entornos con JDK más reciente que el soportado oficialmente por Mockito. | `pom.xml` (`argLine` de Surefire) |
| (commit `8c7649a`) | Unificar convención "not found": adaptadores nunca lanzan `ResourceNotFoundException`, los interactores decidan vía `switchIfEmpty` | Mantener los adaptadores "tontos" (solo mapeo de datos) y la lógica de negocio (qué constituye "no encontrado") en la capa de aplicación. | `ContractInteractor`, `SignatureInteractor`, adaptadores `db/*` |
| (commits `2bfe5b6`, `58bbc69`, `f6de6e6`) | Dejar de silenciar errores de (de)serialización JSON; introducir `PersistenceSerializationException` | Los payloads JSONB son evidencia legal/de integridad de datos; un fallo silencioso podría producir registros corruptos sin que nadie se enterara. | `PersistenceSerializationException`, adaptadores `db/*` |
| (commit `67ce443`) | Apuntar CI/CD (`ci.yml`/`cd.yml`) a `master` en lugar de `main` | Alinear el nombre de la rama principal con la convención real del repositorio. | `.github/workflows/ci.yml`, `.github/workflows/cd.yml` |

## Convenciones Heredadas y Anti-patrones

- **Cero APIs de terceros para biometría/OCR**: evitar OCR o biometría basados en la nube; todo debe ser local/autoalojado (Tess4j, ONNX Runtime local).
- **Verificación explícita**: la lógica de verificación MRZ debe incluir validación de checksum por campo, no solo presencia de los campos.
- **Adaptadores "tontos"**: los adaptadores de infraestructura (`*RepositoryAdapter`) solo deben mapear dominio↔entidad y propagar errores; las decisiones de negocio (qué es "no encontrado", qué estado es válido) viven en los interactores/casos de uso.
- **Anti-patrón activo a evitar al tocar `KycSessionEntity`/`KycRepositoryAdapter`**: no añadir nuevos estados al enum `KycStatus` sin verificar primero que el CHECK constraint de BD (`ck_kyc_status`) los admite — ya existe una divergencia sin resolver con `MRZ_FAILED`/`BIOMETRIC_FAILED` (ver Base de Conocimiento).
- **Anti-patrón a evitar en nuevos adaptadores JSON**: no construir JSON manualmente por concatenación de strings (ver `SignatureRepositoryAdapter`); usar siempre `ObjectMapper` y propagar `PersistenceSerializationException` en caso de fallo, siguiendo el patrón ya establecido en `ContractRepositoryAdapter`/`KycRepositoryAdapter`/`AuditTrailRepositoryAdapter`.

---

### Navegación

- [CLAUDE.md](../CLAUDE.md)
- [context/business_logic.md](../context/business_logic.md)
- [context/database.md](../context/database.md)
- [docs/status.md](../docs/status.md)
