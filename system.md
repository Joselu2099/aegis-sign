# Visión General del Sistema, Tecnologías y Configuración

## Visión General

`aegis-sign` es un microservicio reactivo de Identidad y Cumplimiento dentro del ecosistema TOKENOVO (5 repositorios separados). Provee dos capacidades principales autoalojadas, sin dependencias de APIs de IA en la nube:

1. **KYC (Know Your Customer)**: verificación de identidad mediante OCR local, validación de zona MRZ (ICAO Doc 9303) y verificación biométrica facial 1:1 con prueba de vida.
2. **Firma Electrónica Avanzada (FEA)**: compilación de contratos PDF desde plantillas JSON, firma criptográfica X.509 (BouncyCastle) y generación de un audit trail legal consolidado con su propio PDF firmado.

Arquitectura hexagonal (puertos y adaptadores), 100% reactiva (Spring WebFlux + R2DBC), pensada para alta concurrencia y manejo de archivos binarios (imágenes, PDFs) sin bloquear hilos.

## Stack Tecnológico Completo y Versiones

> Versiones extraídas directamente de `pom.xml` (verificado, no asumido).

- **Lenguaje base**: Java 21 (`java.version=21`).
- **Framework**: Spring Boot 3.2.5 (`spring-boot-starter-parent`), Spring Cloud 2023.0.1.
- **Web/Runtime reactivo**: Spring WebFlux (`spring-boot-starter-webflux`).
- **Persistencia relacional**: Spring Data R2DBC (`spring-boot-starter-data-r2dbc`) + `r2dbc-postgresql` + driver JDBC `postgresql` (usado solo por Flyway).
- **Base de datos**: PostgreSQL (imagen `postgres:16-alpine` en `docker-compose.yml`).
- **Migraciones de esquema**: Flyway (`flyway-core`, sin versión explícita — hereda del BOM de `spring-boot-starter-parent` 3.2.5).
- **Caché/Rate limiting**: Redis reactivo (`spring-boot-starter-data-redis-reactive`); imagen `redis:7-alpine` en `docker-compose.yml`.
- **Almacenamiento de objetos**: MinIO, cliente Java `io.minio:minio:9.0.1`; imagen `minio/minio:RELEASE.2024-03-30T09-41-56Z`.
- **OCR**: Tess4j (`net.sourceforge.tess4j:tess4j:5.11.0`), requiere binarios nativos de Tesseract + Leptonica instalados en el sistema (`tesseract-ocr`, `libtesseract-dev`, `libleptonica-dev`).
- **Generación de PDF**: OpenPDF (`com.github.librepdf:openpdf:3.0.4`).
- **Criptografía**: BouncyCastle (`org.bouncycastle:bcprov-jdk18on:1.78`, `bcpkix-jdk18on:1.78`).
- **Biometría/ML**: ONNX Runtime (`com.microsoft.onnxruntime:onnxruntime:1.17.1`) para embeddings faciales (con fallback simulado si no hay modelo `.onnx` presente — ver `notes/memory.md`).
- **Documentación API**: springdoc-openapi 2.5.0 (`springdoc-openapi-starter-webflux-ui`).
- **Observabilidad**: Micrometer + Prometheus (`micrometer-registry-prometheus`), tracing distribuido vía Brave/Zipkin (`micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`), Spring Boot Actuator.
- **Gestión de secretos**: Spring Cloud Vault Config (`spring-cloud-starter-vault-config`) — la configuración se importa desde Vault en el arranque (`spring.config.import: vault://`).
- **Utilidades**: Lombok 1.18.46, Spring AOP.
- **Build**: Maven (`maven-compiler-plugin`, `spring-boot-maven-plugin`, `jacoco-maven-plugin` 0.8.12 para cobertura).
- **Testing**: JUnit 5 + Mockito (`spring-boot-starter-test`), Reactor Test, Testcontainers 1.19.7 (PostgreSQL, Redis vía `com.redis:testcontainers-redis:2.2.2`), ArchUnit 1.2.1 (`archunit-junit5`) para tests de arquitectura.
- **Contenedorización**: Docker multi-stage (`maven:3.9-eclipse-temurin-21` para build, `eclipse-temurin:21-jre-alpine` para runtime).

## Integraciones Externas y Dependencias de Servicios

- **HashiCorp Vault**: fuente de configuración/secretos en arranque (`VAULT_URI`, `VAULT_TOKEN`); falla rápido si no está disponible (`fail-fast: true`).
- **PostgreSQL 16**: base de datos relacional principal (reactiva vía R2DBC, migraciones vía JDBC).
- **Redis 7**: rate limiting (Token Bucket vía Lua script) y caché de sesión (helper genérico sin consumidores activos confirmados).
- **MinIO**: almacenamiento de objetos S3-compatible, dos buckets (`aegis-sign` permanente, `aegis-sign-temp` temporal con purga GDPR automática).
- **Zipkin**: recolector de trazas distribuidas (`http://localhost:9411` por defecto).
- **Prometheus**: scraping de métricas vía `/actuator/prometheus`.
- **Dependencias internas del ecosistema TOKENOVO**: `aegis-sign` es uno de 5 repositorios independientes del ecosistema; no se encontraron referencias a librerías compartidas o llamadas HTTP salientes a otros servicios TOKENOVO en el código actual (los puntos de integración observados son exclusivamente con la infraestructura listada arriba).

## Configuración del Entorno y Prerrequisitos

- **Sistema operativo**: cualquiera con soporte Docker (desarrollo verificado en macOS); imagen runtime basada en Alpine Linux.
- **Requisitos del sistema**:
  - JDK 21 (Temurin recomendado, según CI y Dockerfile).
  - Maven 3.9+.
  - Docker y Docker Compose (para PostgreSQL, Redis, MinIO locales).
  - Librerías nativas de Tesseract OCR + Leptonica instaladas en el host si se ejecuta fuera de contenedor (`tesseract-ocr`, `libtesseract-dev`, `libleptonica-dev` en Debian/Ubuntu).
  - Instancia de HashiCorp Vault accesible (o `VAULT_TOKEN` configurado) para resolver secretos en el arranque.
- **Herramientas de desarrollo local**: VS Code (`.vscode/` presente en el repo).

## Pasos Completos de Instalación y Ejecución

1. Levantar la infraestructura de soporte: `docker-compose up -d postgres redis minio` (o `docker-compose up` para incluir también la app dockerizada).
2. Asegurar que Vault esté accesible y `VAULT_TOKEN` esté exportado (o usar `VAULT_TOKEN: dummy-ci-token` solo para flujos de CI/test).
3. Compilar y ejecutar tests: `mvn clean test`.
4. Ejecutar la aplicación en local: `mvn spring-boot:run` (lee `src/main/resources/application.yml`, con `db.username`/`db.password`/`minio.access-key`/`minio.secret-key`/`keystore.password`/`keystore.key-password` resueltos vía Vault).
5. Verificar el arranque: `GET http://localhost:8080/actuator/health`.
6. Documentación interactiva de la API (OpenAPI/Swagger UI vía springdoc): disponible en el path estándar de springdoc-openapi-starter-webflux-ui una vez la app está arriba.
7. Build de imagen Docker completa: `docker build -t aegis-sign .` o `docker-compose up --build app`.

## Comandos Operativos Clave

| Comando | Propósito | Contexto/Módulo |
|---------|-----------|------------------|
| `mvn clean test` | Ejecuta toda la suite de tests (unitarios + arquitectura ArchUnit) con cobertura JaCoCo. | Raíz del proyecto |
| `mvn clean package -DskipTests` | Genera el JAR ejecutable sin correr tests (usado en `cd.yml`). | Raíz del proyecto |
| `mvn test -Pintegration-tests` | Ejecuta tests de integración contra servicios reales en `localhost` (perfil que deshabilita Testcontainers y usa las credenciales fijas definidas en `pom.xml`). | Perfil Maven `integration-tests` |
| `docker-compose up -d` | Levanta PostgreSQL, Redis, MinIO y la app empaquetada. | Raíz del proyecto |
| `docker build -t aegis-sign .` | Construye la imagen Docker multi-stage. | `Dockerfile` |

---

### Contexto y Navegación

- [CLAUDE.md](CLAUDE.md)
- [AGENTS.md](AGENTS.md)
- [context/architecture.md](context/architecture.md)
- [docs/api-guide.md](docs/api-guide.md) — referencia detallada de endpoints REST.
- [docs/requirements.md](docs/requirements.md) — requisitos funcionales/no funcionales originales.
