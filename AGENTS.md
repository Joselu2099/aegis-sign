# Reglas y Flujos de Trabajo del Agente

## Reglas Fundamentales de Programación

> [!IMPORTANT]
> **Reutilización y Consistencia de Código**: Siempre priorizar la reutilización de código y patrones de implementación existentes. El código nuevo debe mantener el mismo estilo, estructura y lógica que la base de código actual (ej. arquitectura hexagonal, adaptadores "tontos", convención unificada de excepciones "not found" — ver `notes/memory.md`).
>
> **Cero Reformateo**: Está estrictamente prohibido cambiar la codificación del archivo, la indentación (espacios vs. tabs), o realizar reformateo general de código o archivos existentes. Cualquier modificación debe ser quirúrgicamente precisa.

## Reglas de Ramas y Versionado (OBLIGATORIO)

> [!IMPORTANT]
> - **Nunca** hacer commit ni push directo a `master`. Todo trabajo se realiza en una rama dedicada (`feature/*`, `fix/*`, `refactor/*`) creada desde `master` o desde la rama `release/x.y.z-snapshot` activa.
> - Antes de fusionar a `master`, la versión en `pom.xml` (`<version>`) debe reflejar el incremento correspondiente (ej. `0.0.2-SNAPSHOT` → `0.0.2` para liberar, o bump a la siguiente `-SNAPSHOT` para seguir desarrollando). El pipeline `cd.yml` solo crea tag/release/imagen Docker si la versión en `pom.xml` ya NO tiene el sufijo `-SNAPSHOT` y el tag `vX.Y.Z` no existe aún.
> - Usar `git worktree` para aislar el trabajo de cada rama de feature cuando se trabaje en paralelo (carpeta `.worktrees/`, ya ignorada en `.gitignore`).

## Patrón CI/CD (`ci.yml` / `cd.yml`)

- **`ci.yml`** (Validación): se ejecuta en cada push a cualquier rama y en cada PR contra `master`. Instala dependencias nativas de Tesseract OCR, ejecuta `mvn clean test`, sube reportes Surefire si hay fallos, y genera un resumen de cobertura JaCoCo en el step summary de GitHub Actions.
- **`cd.yml`** (Entrega Continua): se ejecuta solo en push a `master`. Verifica la versión de `pom.xml`; si no es `-SNAPSHOT` y el tag no existe, crea el tag `vX.Y.Z`, publica un GitHub Release con el JAR adjunto (`mvn clean package -DskipTests`), y construye/publica la imagen Docker en GHCR (`ghcr.io/<repo>:vX.Y.Z` y `:latest`).
- Este patrón (`ci.yml` + `cd.yml` con auto-tag y auto-release) es compartido entre los 5 repositorios del ecosistema TOKENOVO; mantener la convención si se modifican los workflows.

## Flujo de Desarrollo de Funcionalidades (Subagentes + Revisión en Dos Etapas)

Este repositorio usa desarrollo dirigido por subagentes (`subagent-driven-development`) combinado con worktrees aislados para trabajo de features:

1. **Brainstorming**: ninguna escritura de código hasta que el diseño esté aprobado (skill `brainstorming`).
2. **Planificación**: crear un plan detallado (skill `writing-plans`), registrado en un archivo de plan (ej. `implementation_plan.md` o `tasks.md`, ya presente en la raíz del repo).
3. **Ejecución aislada**: ejecutar el plan en un worktree dedicado (skill `using-git-worktrees` / `executing-plans`), manteniendo `master` y la copia de trabajo principal limpios.
4. **Revisión en dos etapas** (obligatoria antes de fusionar):
   - **Etapa 1 — Cumplimiento de la especificación**: verificar que la implementación cumple exactamente lo planificado/especificado, sin desviaciones no acordadas.
   - **Etapa 2 — Calidad de código**: revisión de estilo, reutilización, simplicidad, eficiencia y ausencia de regresiones (skills `code-review` / `receiving-code-review`).
5. Solo tras superar ambas etapas se fusiona a la rama `release/*` activa o a `master`, respetando el bump de versión en `pom.xml`.

## Roles y Pautas de Agentes

- **codebase_investigator**: usado para análisis arquitectónico profundo y para encontrar patrones en los flujos reactivos (Mono/Flux) existentes antes de modificarlos.
- **generalist**: usado para operaciones por lotes o refactorizaciones complejas de adaptadores de infraestructura.

## Hallazgos Técnicos Relevantes para el Desarrollo

Antes de tocar el módulo KYC o de persistencia, revisar `notes/memory.md` — contiene divergencias activas conocidas entre código y base de datos (ej. CHECK constraint `ck_kyc_status` vs. mapeo de `MRZ_FAILED`/`BIOMETRIC_FAILED`) que deben tenerse en cuenta para no introducir regresiones silenciosas.

---

### Contexto y Navegación

- [CLAUDE.md](CLAUDE.md)
- [system.md](system.md)
- [context/architecture.md](context/architecture.md)
- [notes/memory.md](notes/memory.md)
