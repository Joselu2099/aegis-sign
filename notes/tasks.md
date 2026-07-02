# 📋 Backlog de Tareas: aegis-sign

> Generado con la skill `structuring-task-backlogs`. Sustituye por completo la versión anterior de este archivo (formato ad-hoc, fechada 2026-06-08, con bloques 0–7 y TASK-01..24) — aquella numeración quedó obsoleta: varias tareas que marcaba como "pendiente" (tessdata Docker, matching biométrico, audit trail PDF) ya están completadas según `notes/memory.md` (auditoría de código más reciente, 2026-06-24/26). No se conserva el histórico aquí; ver `git log -- notes/tasks.md` si se necesita.
>
> Para el roadmap y fases a nivel de todo el ecosistema TOKENOVO, ver [`../../tokenovo-core/notes/roadmap.md`](../../tokenovo-core/notes/roadmap.md) y [`Fase_1_Detallada.md`](../../tokenovo-core/notes/Fase_1_Detallada.md). Este archivo cubre solo el backlog verificable de **este repositorio**.

## Metadatos

- **Proyecto:** aegis-sign
- **Fase:** Fase 1 (Financiación Core) — Bloque 1: Identidad y Firma
- **Inicio del backlog actual:** 2026-06-26
- **Responsable:** Jose Luis Sánchez Carrasco

---

## CLIs y LLMs disponibles en esta máquina

> Regenerar con `bash ~/.claude/skills/structuring-task-backlogs/scripts/detect-clis.sh` — no mantener esta tabla a mano.

| CLI | Modelos | Notas |
| --- | --- | --- |
| `claude` | Según plan/login activo (p. ej. claude-sonnet-4.6, claude-opus-4.8, claude-haiku-4.5) | El CLI no expone listado de modelos por comando; confirmar contra el plan activo. |
| `agy` | Multi-modelo (Gemini, Claude, GPT-OSS, etc. según configuración) | Confirmar catálogo vigente con `agy models` si el CLI lo soporta. |
| `opencode` | Catálogo amplio de modelos gratuitos y de pago (Claude, GPT-5.x, Gemini, DeepSeek, GLM, Kimi, Qwen, etc.) | Ejecutar `opencode models` para el listado completo y vigente. |
| `ollama` | (ninguno descargado en esta máquina a fecha de generación) | Ejecutar `ollama pull <modelo>` antes de asignarle una tarea. |

---

## 🛠️ BACKLOG DE TAREAS

### TASK-01: Calibrar `matchThreshold` del matcher biométrico con dataset real

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Alta
- **Estado:** Pendiente
- **Estimación de Costo:** 15k–25k Tokens (más el tiempo de obtener/curar el dataset, fuera del alcance de tokens)
- **Dependencias:** Ninguna (el wiring real de `BiometricMatchingService` en el flujo KYC ya está completo)

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** Claude Opus 4.6 (Thinking) vía `agy` — tier 3 (juicio sobre trade-off FP/FN), pero al ser `user-driven` (el humano provee y valida el dataset) conviene diversificar el CLI en vez de reservar la cuota de `claude` directo para esto
- **CLI y Comando:** `agy -p "Calibra el matchThreshold de BiometricMatchingService usando el dataset real proporcionado en <ruta>, documenta la matriz de falsos positivos/negativos resultante" --model "Claude Opus 4.6 (Thinking)" --add-dir .`
- **Workflow:** `user-driven`
  - *No es subagent-driven porque requiere que un humano provea y valide el dataset real — no se puede simular de forma fiable.*
- **Skills:** `java-development-guide`, `verification-before-completion`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] Existe un modelo `.onnx` real desplegado en `biometrics.model-path` (hoy el directorio `src/main/resources/models/` no existe — el matching cae siempre en `calculateMockSimilarity`)
- [ ] `matchThreshold` (default 0.8) ajustado y documentado contra resultados de un dataset real, no sintético
- [ ] Matriz de falsos positivos/negativos registrada en `notes/memory.md`

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio. Bloqueante antes de onboarding de usuarios reales, según `roadmap.md` §5 — no bloquea el merge a `master`.)*

---

### TASK-02: Persistencia real de KeyStore (PKCS12) para `EncryptionPort`

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Alta
- **Estado:** Pendiente
- **Estimación de Costo:** 15k–20k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** claude-opus-4.8 — tier 3 (decisión de diseño de seguridad/almacenamiento de claves); se queda en `claude` porque `Workflow: subagent-driven-development` solo puede ejecutarse vía el Agent tool del orquestador, que únicamente lanza subagentes Claude
- **CLI y Comando:** `claude -p "Implementa persistencia real de KeyStore PKCS12 para SoftwareKeyStoreEncryptionAdapter, sustituyendo la clave AES-256 efímera en memoria"`
- **Workflow:** `subagent-driven-development`
  - **Spec compliance:** ⏳ Pendiente
  - **Code quality:** ⏳ Pendiente
- **Skills:** `java-development-guide`, `writing-plans`, `verification-before-completion`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] `SoftwareKeyStoreEncryptionAdapter` carga la clave desde un `.p12` (o almacén externo) en vez de generarla en memoria con `KeyGenerator` en cada arranque
- [ ] Reiniciar el contenedor no invalida `certificateThumbprint`s cifrados antes del reinicio
- [ ] Contraseña del KeyStore configurable vía variable de entorno, no hardcodeada

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio. Hallazgo verificado en `notes/memory.md`: la clase vive en `infrastructure/adapter/keystore/` pero nunca toca un `.p12` real.)*

---

### TASK-03: Corregir CHECK constraint `ck_kyc_status` vs. mapeo de estados `FAILED`

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Alta
- **Estado:** ✅ Completado
- **Commit:** `816f75b` (rama `feature/ck-kyc-status-check`)
- **Estimación de Costo:** 5k–8k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** Gemini 3.5 Flash (Medium) vía `agy` — tier 2 (corrección estándar, sin ambigüedad de diseño una vez identificada la causa)
- **CLI y Comando:** `agy -p "Corrige la divergencia entre KycRepositoryAdapter.mapStatusToDb (mapea MRZ_FAILED/BIOMETRIC_FAILED a 'FAILED') y el CHECK constraint ck_kyc_status de V2__update_kyc_status.sql (que ya no admite 'FAILED'), con una migración V4 o cambiando el mapeo a REJECTED" --model "Gemini 3.5 Flash (Medium)" --add-dir .`
- **Workflow:** `multi-cli-subagent-dispatch`
  - **Spec compliance:** ⏳ Pendiente
  - **Code quality:** ⏳ Pendiente
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [x] Una sesión KYC que falla validación MRZ o biométrica se persiste sin violar el CHECK constraint en PostgreSQL real (no solo en tests con mocks de repositorio)
- [x] Migración V4 documentada en `db/migration/V4__add_failed_to_kyc_status_check.sql` — amplía el constraint para incluir `'FAILED'`
- [x] Test de integración `KycRepositoryIntegrationTest` (hereda `AbstractIntegrationTest`, 4 tests) que reproduce el escenario de fallo contra PostgreSQL real via Testcontainers

#### 4. 📝 NOTAS DE EJECUCIÓN

> Completado 2026-06-28. Migración V4 redefine `ck_kyc_status` para incluir `'FAILED'`. Test de integración con 4 casos: MRZ_FAILED, BIOMETRIC_FAILED, round-trip y regresión de APPROVED.

---

### TASK-04: Handler dedicado para `PersistenceSerializationException`

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Media
- **Estado:** ✅ Completado
- **Estimación de Costo:** 5k–8k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** modelo gratuito de `opencode` (p. ej. `opencode/deepseek-v4-flash-free`) — tier 1, cambio mecánico con spec totalmente clara, no justifica gastar cuota de pago
- **CLI y Comando:** `opencode run "Añade un @ExceptionHandler dedicado para PersistenceSerializationException en GlobalExceptionHandler con código de error específico PERSISTENCE_SERIALIZATION_ERROR" -m opencode/deepseek-v4-flash-free`
- **Workflow:** `multi-cli-subagent-dispatch`
  - **Spec compliance:** ✅ Aprobado
  - **Code quality:** ✅ Aprobado
- **Skills:** `java-development-guide`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [x] `GlobalExceptionHandler` tiene un `@ExceptionHandler(PersistenceSerializationException.class)` específico
- [x] La respuesta incluye `errorCode = "PERSISTENCE_SERIALIZATION_ERROR"`, distinguible del 500 genérico
- [x] Test que verifica el código de error en la respuesta cuando se fuerza un fallo de (de)serialización

#### 4. 📝 NOTAS DE EJECUCIÓN

> **2026-06-27:** Ejecutado vía `multi-cli-subagent-dispatch` (`opencode/deepseek-v4-flash-free`). Handler añadido siguiendo el mismo patrón `ApiResponse.error(...)` que el resto de `GlobalExceptionHandler`; test nuevo en `ContractControllerTest` verifica `errorCode = "PERSISTENCE_SERIALIZATION_ERROR"`. Verificado por el orquestador vía `git diff` (cambio mínimo, sin extras) — **91/91 tests pasan**, `BUILD SUCCESS`. Commit `a5cae57` en rama `feature/persistence-serialization-handler` — sin mergear.

---

### TASK-05: `UNIQUE` constraint en `audit_trails.contract_id` + uso de `Contract.markAsSigned()`

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Media
- **Estado:** Pendiente
- **Estimación de Costo:** 8k–12k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** Gemini 3.5 Flash (Medium) vía `agy` — tier 2 (fix con invariante de dominio clara, no requiere decisión de arquitectura)
- **CLI y Comando:** `agy -p "Añade migración Flyway con UNIQUE sobre audit_trails.contract_id y corrige SignatureInteractor.signContract para usar Contract.markAsSigned() en vez de contract.setStatus(SIGNED) directo" --model "Gemini 3.5 Flash (Medium)" --add-dir .`
- **Workflow:** `multi-cli-subagent-dispatch`
  - **Spec compliance:** ⏳ Pendiente
  - **Code quality:** ⏳ Pendiente
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] Nueva migración Flyway añade `UNIQUE` sobre `audit_trails.contract_id`
- [ ] `SignatureInteractor.signContract` invoca `Contract.markAsSigned()` y propaga el `IllegalStateException` de doble firma como error de negocio (no 500)
- [ ] Test que verifica que firmar dos veces el mismo contrato falla de forma controlada

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio. Hallazgo de `notes/memory.md`: hoy nada impide doble firma a nivel de BD ni de dominio en el flujo real.)*

---

### TASK-06: Serialización JSON real en `SignatureRepositoryAdapter` (eliminar concatenación manual de strings)

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Media
- **Estado:** ✅ Completado
- **Estimación de Costo:** 5k–8k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** modelo gratuito de `opencode` (p. ej. `opencode/deepseek-v4-flash-free`) — tier 1, el patrón a replicar ya existe en el propio código (`ContractRepositoryAdapter`/`KycRepositoryAdapter`), no requiere diseño nuevo
- **CLI y Comando:** `opencode run "Sustituye la concatenación manual de strings en SignatureRepositoryAdapter.toEntity/extractSignerId por ObjectMapper real, siguiendo el patrón ya usado en ContractRepositoryAdapter/KycRepositoryAdapter" -m opencode/deepseek-v4-flash-free`
- **Workflow:** `multi-cli-subagent-dispatch`
  - **Spec compliance:** ✅ Aprobado
  - **Code quality:** ✅ Aprobado
- **Skills:** `java-development-guide`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [x] `signerInfo` se serializa con `ObjectMapper.writeValueAsString(...)`, no concatenación de strings
- [x] `extractSignerId` parsea JSON real (no regex) y propaga `PersistenceSerializationException` en caso de fallo
- [x] Test con un `signerId` que contiene comillas dobles, para confirmar que ya no corrompe el JSON

#### 4. 📝 NOTAS DE EJECUCIÓN

> **2026-06-27:** Ejecutado vía `multi-cli-subagent-dispatch` (`opencode/deepseek-v4-flash-free`). `toEntity`/`toDomain` pasaron a devolver `Mono` (la (de)serialización puede fallar), con `save`/`findById`/`findByContractId` actualizados en cascada para mantenerse reactivos — más invasivo de lo esperado pero correcto, no se podía envolver una excepción potencial sin cambiar la firma en un pipeline reactivo. Test nuevo (`SignatureRepositoryAdapterTest`) cubre round-trip, `signerId` con comillas dobles (el caso de corrupción real que motivó la tarea), y propagación de error en ambas direcciones (serialización y deserialización). Verificado por el orquestador vía `git diff` completo — **94/94 tests pasan**, `BUILD SUCCESS`. Commit `b4518ff` en rama `feature/signature-json-serialization` — sin mergear.

---

### TASK-07: Decidir destino de `KycInteractor.verifySession` (código muerto desde la API pública)

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Media
- **Estado:** Pendiente
- **Estimación de Costo:** 5k–10k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** N/A para la decisión (humana); una vez decidido el rumbo, la implementación es tier 1-2 — usar `recommend-cli.sh` de `multi-cli-subagent-dispatch` para elegir CLI/modelo en ese momento, no fijarlo de antemano
- **CLI y Comando:** N/A hasta que exista la decisión registrada
- **Workflow:** `user-driven`
  - *Requiere decisión de producto (¿existe un flujo de revisión manual real?), no solo técnica — por eso no es subagent-driven hasta que el humano decida el rumbo.*
- **Skills:** `java-development-guide`, `writing-plans`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] Decisión registrada en `notes/memory.md` (Registro de Decisiones)
- [ ] Si se mantiene: `verifySession` valida `mrzValid`/`biometricValid` reales y tiene endpoint REST mapeado
- [ ] Si se elimina: el método y su entrada de puerto desaparecen del código

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio.)*

---

### TASK-08: Validar estado KYC `APPROVED` antes de permitir firma

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Alta
- **Estado:** Pendiente
- **Estimación de Costo:** 8k–12k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** claude-sonnet-4.6 — se queda en `claude` a pesar de no ser arquitectónicamente compleja: es un control de seguridad (gate de autenticación antes de firmar), y el coste de revisión consistente vía el Agent tool pesa más que el ahorro de offload aquí
- **CLI y Comando:** `claude -p "Haz explícito en SignatureInteractor.signContract el chequeo de que la KycSession del firmante esté en estado APPROVED antes de firmar, lanzando un error de negocio si no"`
- **Workflow:** `subagent-driven-development`
  - **Spec compliance:** ⏳ Pendiente
  - **Code quality:** ⏳ Pendiente
- **Skills:** `java-development-guide`, `realizar-correctivo`, `verification-before-completion`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] `signContract` consulta el estado real de la `KycSession` del firmante antes de proceder
- [ ] Intento de firma con KYC no `APPROVED` devuelve 422/403 con mensaje claro, no 500 ni firma silenciosa
- [ ] Test que cubre firma con KYC pendiente/rechazado

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio. Riesgo real señalado en `notes/memory.md`: hoy se permite firmar sin identidad verificada.)*

---

### TASK-09: Decidir destino de `RedisSessionCacheHelper` (sin consumidores activos)

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Baja
- **Estado:** ✅ Completado
- **Estimación de Costo:** 3k–5k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** modelo gratuito de `opencode` (p. ej. `opencode/deepseek-v4-flash-free`) — tier 1, verificación mecánica (grep de uso) + decisión trivial
- **CLI y Comando:** `opencode run "Confirma con grep si RedisSessionCacheHelper tiene consumidores reales; si no, elimínalo, o documenta su uso previsto si se decide conservarlo" -m opencode/deepseek-v4-flash-free`
- **Workflow:** `multi-cli-subagent-dispatch`
  - **Spec compliance:** ✅ Aprobado
  - **Code quality:** ✅ Aprobado
- **Skills:** `java-development-guide`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [x] Confirmado por grep que no hay ningún `@Autowired`/inyección de `RedisSessionCacheHelper` en interactores
- [x] Eliminado, o documentado su propósito futuro en `notes/memory.md`

#### 4. 📝 NOTAS DE EJECUCIÓN

> **2026-06-27:** Ejecutado vía `multi-cli-subagent-dispatch` (`opencode run` con `opencode/deepseek-v4-flash-free`, primer dispatch real de validación del pipeline). El orquestador verificó primero con grep (`find src/main -name "*Interactor*.java" | xargs grep -l RedisSessionCacheHelper` → sin resultados) que no había consumidores; el subagente externo repitió la verificación de forma independiente, eliminó `RedisSessionCacheHelper.java` y su test, y confirmó 0 referencias restantes en todo el repo. Build verificado: `mvn -Djacoco.skip=true test` → **84/84 tests pasan**, `BUILD SUCCESS`. Commit `b98fae1` en rama `feature/redis-cache-helper-cleanup` (worktree `.worktrees/redis-cache-helper-cleanup`, branched desde `release/0.0.2-snapshot`) — **sin mergear**, pendiente de PR y revisión humana como el resto de ramas de este repo.

---

### TASK-10: Soporte Docker local para tests de integración con Testcontainers

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Baja
- **Estado:** Pendiente
- **Estimación de Costo:** 10k–15k Tokens
- **Dependencias:** Ninguna

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** claude-sonnet-4.6
- **CLI y Comando:** `claude -p "Verifica y documenta el perfil integration-tests de pom.xml para que los tests con Testcontainers corran de forma fiable con Docker local"`
- **Workflow:** `manual`
  - *Depende del entorno Docker local del desarrollador — no tiene sentido delegarlo a un subagente sin acceso garantizado a Docker.*
- **Skills:** `java-development-guide`

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] Tests de integración con Testcontainers corren sin fallos con Docker disponible
- [ ] Se saltan limpiamente (sin crash) cuando Docker no está disponible, como ya ocurre hoy según el histórico del repo

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente de inicio.)*

---

### TASK-11: Mergear `release/0.0.2-snapshot` → `master`

#### 1. 📊 EVALUACIÓN INICIAL

- **Prioridad:** Alta
- **Estado:** Bloqueado (esperando autorización humana)
- **Estimación de Costo:** N/A — no es trabajo de implementación
- **Dependencias:** Ninguna técnica; bloqueada por política del proyecto

#### 2. ⚙️ ASIGNACIÓN DE INFRAESTRUCTURA

- **LLM Seleccionado:** N/A
- **CLI y Comando:** `gh pr merge <PR> --merge` (solo tras revisión y aprobación humana explícita del PR)
- **Workflow:** `user-driven`
  - *Ningún agente hace merge a `master` sin autorización explícita — regla transversal del ecosistema, ver `AGENTS.md`.*
- **Skills:** —

#### 3. ✅ CRITERIOS DE ACEPTACIÓN

- [ ] PR de `release/0.0.2-snapshot` → `master` revisado y aprobado por un humano
- [ ] Tras el merge, `cd.yml` genera el primer tag/release real (`v0.1.0`) automáticamente

#### 4. 📝 NOTAS DE EJECUCIÓN

> *(Pendiente. Esta es, literalmente, la primera versión publicada del proyecto — ver `roadmap.md` §2.)*

---

> **Estado del Backlog:** 27% Completado (3/11) | 0 En Progreso | 1 Bloqueada (TASK-11, espera humana) | Fase 1, Bloque 1: ✅ funcionalmente completo, pendiente de calibración/hardening y merge
>
> *Nota: el trabajo ya completado de este repositorio (OCR/MRZ, biometría 1:1 conectada al flujo real, firma X.509, audit trail PDF, purga GDPR, unificación de convención "not found", fin del silenciamiento de errores JSON) no se relista aquí como tareas — está descrito como hechos verificados en `notes/memory.md` § Base de Conocimiento y Backlog (filas `DONE`). Este backlog solo contiene trabajo pendiente real, recalculado contando directamente, no de memoria.*