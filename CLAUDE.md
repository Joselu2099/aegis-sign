# CLAUDE.md (Hub del Proyecto)

Este archivo actúa como el punto de navegación central del repositorio `aegis-sign`. Consulta los documentos específicos abajo para guía detallada. Equivalente para otros agentes: [GEMINI.md](GEMINI.md).

## Documentación Central

- **[AGENTS.md](AGENTS.md)**: reglas, flujos de trabajo y guías de programación (ramas, versionado, CI/CD, desarrollo con subagentes y worktrees).
- **[system.md](system.md)**: stack tecnológico exacto, instalación, configuración del entorno y comandos operativos.

## Contexto del Proyecto

Contexto técnico detallado y exhaustivo en `context/`:

- **Arquitectura**: [context/architecture.md](context/architecture.md) — capas, módulos, diagrama de ciclo de vida de solicitud/datos.
- **Lógica de Negocio**: [context/business_logic.md](context/business_logic.md) — inventario completo de los 5 modelos de dominio, diagrama de clases, reglas de negocio y flujos funcionales.
- **Base de Datos**: [context/database.md](context/database.md) — inventario completo de las 4 tablas R2DBC/Flyway, diagrama ERD, relaciones e integridad de datos.

## Memoria y Notas

Seguimiento operativo y hallazgos técnicos vivos:

- **Memoria del Proyecto**: [notes/memory.md](notes/memory.md) — backlog, decisiones técnicas, divergencias código/BD detectadas, convenciones y anti-patrones.

## Documentación Suplementaria (`docs/`)

Documentos previos al proceso de estandarización, conservados por su valor histórico/funcional y referenciados por commits anteriores:

- **[docs/status.md](docs/status.md)**: snapshot del estado del proyecto en un momento dado.
- **[docs/goal.md](docs/goal.md)**: especificación/objetivo original del proyecto.
- **[docs/requirements.md](docs/requirements.md)**: requisitos funcionales y no funcionales (RF/RNF).
- **[docs/api-guide.md](docs/api-guide.md)**: referencia de la API REST con ejemplos de petición/respuesta.
- **[docs/history/](docs/history/)**: snapshots históricos de estado (archivo, no modificar).
- **[docs/superpowers/](docs/superpowers/)**: herramientas/skills no relacionadas con el dominio del negocio (no modificar como parte de tareas de documentación de dominio).

> Nota: `docs/architecture.md`, `docs/business-logic.md`, `docs/database.md` y `docs/memory.md` fueron migrados y actualizados a `context/` y `notes/` (estructura estandarizada AI-agnóstica) y eliminados de `docs/` para evitar duplicidad de contenido desactualizado.

---

### Contexto y Navegación

- [AGENTS.md](AGENTS.md)
- [system.md](system.md)
- [context/architecture.md](context/architecture.md)
