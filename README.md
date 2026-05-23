# aegis-sign

Servicio crítico autohospedado para la gestión del ciclo de vida de identidad (KYC) y la ejecución de Firma Electrónica Avanzada (FEA) bajo un esquema PKI descentralizado y de alta concurrencia.

---

## 1. Descripción Ejecutiva
`aegis-sign` es un microservicio crítico de backend diseñado para operar de manera 100% autónoma y autohospedada (self-hosted), eliminando la dependencia de proveedores externos (e.g., DocuSign, Sumsub, Adobe Sign) en la verificación de identidad y la firma de documentos. El servicio provee dos capacidades fundacionales para ecosistemas financieros tradicionales, FinTech y plataformas blockchain:
1. **Know Your Customer (KYC):** Un flujo automatizado y asíncrono para la ingesta y validación de documentos de identidad, extracción de metadatos mediante OCR e inspección biométrica de similitud facial cara-a-documento.
2. **Firma Electrónica Avanzada (FEA):** Orquestación criptográfica para la preparación de contratos basados en cláusulas modulares, hashing determinista (SHA-256), sellado criptográfico mediante una infraestructura de clave pública (PKI) interna basada en certificados X.509 y generación automatizada de pistas de auditoría legales inmutables.

Diseñado bajo el paradigma reactivo y los principios de la Arquitectura Hexagonal, el sistema garantiza un procesamiento no bloqueante de alta concurrencia en la transferencia de flujos binarios pesados (documentos y firmas).

---

## 2. Características Principales (Core Features)

### Módulo KYC
*   **Pipeline de Ingesta Documental:** Orquestación reactiva para la subida asíncrona de archivos multiparte de alta resolución (DNI, Pasaportes, Licencias).
*   **OCR & Metadata Extraction:** Motor local de reconocimiento óptico de caracteres para extraer de forma estructurada campos críticos (nombre, apellidos, número de identidad, fecha de nacimiento, fecha de vencimiento) y validar checksums MRZ (Machine Readable Zone).
*   **Inspección Biométrica Cara-a-Documento:** Comparación facial 1:1 local basada en redes neuronales convolucionales para verificar la correspondencia entre la fotografía del documento cargado y el selfie capturado en tiempo real por el usuario.

### Módulo de Firma Electrónica
*   **Renderizado de Contratos Modulares:** Compilación en tiempo real (síncrona/asíncrona) de plantillas PDF a partir de fragmentos o cláusulas almacenadas en la base de datos y variables de contexto dinámicas.
*   **Hashing Criptográfico (SHA-256):** Generación determinista del hash representativo del estado exacto del contrato antes de la firma, asegurando el no repudio y la integridad absoluta de los datos.
*   **Sellado con Certificados X.509:** Aplicación de firma digital avanzada sobre el PDF (PAdES) utilizando llaves privadas y certificados criptográficos emitidos y gestionados por la PKI interna de la organización.
*   **Generación de Audit Trail Legal:** Generación automática de un manifiesto JSON/PDF firmado que registra de forma inmutable la traza de auditoría (direcciones IP, hashes intermedios, timestamps, datos del dispositivo, resultado del KYC y consentimiento explícito).

---

## 3. Arquitectura Interna y Stack Tecnológico

El microservicio está implementado sobre una base reactiva de baja latencia y alta escalabilidad:

*   **Runtime & Framework:** Java 21 y **Spring Boot 3.x**.
*   **Motor Reactivo:** **Spring WebFlux** (Project Reactor) y Netty como servidor embebido para optimizar la entrada/salida no bloqueante en endpoints de alta carga y procesamiento de archivos.
*   **Almacenamiento Persistente (Auditoría):** **PostgreSQL** para la persistencia del Audit Trail, histórico de firmas, plantillas y metadatos de las sesiones KYC.
*   **Almacenamiento en Caché y Estado (KYC):** **Redis** para la gestión del estado efímero de las sesiones KYC (duración limitada del token de sesión, estado de validación y almacenamiento temporal de resultados OCR).
*   **Almacenamiento de Objetos (Private Cloud):** **MinIO** (compatible con S3 API) configurado de forma interna para almacenar de forma segura PDFs en crudo, plantillas de contratos y las imágenes/selfies utilizadas en los flujos KYC.

```
┌────────────────────────────────────────────────────────┐
│                   Netty (Spring WebFlux)               │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
      [ Módulo KYC ]            [ Módulo Firma ]
             │                           │
   ┌─────────┼─────────┐                 ├──────────────┐
   ▼         ▼         ▼                 ▼              ▼
[Redis]   [MinIO]   [OCR/Bio]      [PostgreSQL]      [PKI]
```

---

## 4. Diseño de la API REST (Endpoints Clave)

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/v1/kyc/sessions` | Inicializa una sesión efímera de KYC y retorna el token de sesión y los requisitos de validación. |
| `POST` | `/api/v1/kyc/sessions/{id}/documents` | Carga el documento de identidad (anverso/reverso) para ejecutar el procesamiento OCR y la validación de MRZ de forma reactiva. |
| `POST` | `/api/v1/kyc/sessions/{id}/biometrics` | Carga el selfie biométrico y realiza la comparación facial contra la imagen del documento de identidad guardada en la sesión. |
| `POST` | `/api/v1/signatures/prepare` | Recibe la estructura de cláusulas y metadatos, renderiza el documento contractual PDF final en MinIO y retorna su URI y hash SHA-256. |
| `POST` | `/api/v1/signatures/sign` | Registra el consentimiento explícito y la firma, estampa el sello digital X.509 en el PDF y compila y cierra el Audit Trail. |

---

## 5. Estructura del Proyecto

El código fuente sigue los principios de la **Arquitectura Hexagonal (Puertos y Adaptadores)** para aislar la lógica de dominio de las dependencias de infraestructura y frameworks:

```
aegis-sign/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── aegis/
│   │   │           └── sign/
│   │   │               ├── domain/                 # Lógica de dominio pura (Entidades, Reglas y Value Objects)
│   │   │               │   ├── model/              # Entidades (KycSession, Contract, Signature, AuditTrail)
│   │   │               │   └── exception/          # Excepciones de dominio
│   │   │               │
│   │   │               ├── application/            # Casos de uso y Puertos de la aplicación
│   │   │               │   ├── port/
│   │   │               │   │   ├── in/             # Casos de uso (IniciarKycUseCase, FirmarContratoUseCase)
│   │   │               │   │   └── out/            # Interfaces de salida (KycRepositoryPort, DocumentStorePort)
│   │   │               │   └── service/            # Implementaciones de la lógica de aplicación
│   │   │               │
│   │   │               ├── infrastructure/         # Adaptadores externos y configuraciones
│   │   │               │   ├── adapter/
│   │   │               │   │   ├── input/          # Controladores REST reactivos y deserializadores
│   │   │               │   │   └── output/         # Repositorios R2DBC (PostgreSQL), Redis Client, MinIO Client, PKI Adapter
│   │   │               │   └── config/             # Configuración de beans, base de datos reactiva, seguridad y clientes
│   │   │               │
│   │   │               └── AegisSignApplication.java # Clase de inicio de Spring Boot
│   │   └── resources/
│   │       ├── application.yml                     # Parámetros del entorno e infraestructura reactiva
│   │       └── db/migration/                       # Scripts de migración de base de datos (Flyway/Liquibase)
│   └── test/                                       # Pruebas unitarias, de integración y de arquitectura (ArchUnit)
```

---

## 6. Requisitos Previos y Guía de Inicio Rápido

### Infraestructura Local con Docker Compose
Para levantar la infraestructura mínima de soporte localmente (PostgreSQL, Redis, MinIO), crea un archivo `docker-compose.yml` en la raíz del proyecto y ejecuta el siguiente comando:

```bash
docker compose up -d
```

#### Configuración Mínima (`docker-compose.yml` de referencia):
```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: aegis-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: aegis_db
      POSTGRES_USER: aegis_user
      POSTGRES_PASSWORD: aegis_password

  redis:
    image: redis:7-alpine
    container_name: aegis-redis
    ports:
      - "6379:6379"

  minio:
    image: minio/minio:RELEASE.2024-03-30T09-41-56Z
    container_name: aegis-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: aegis_admin
      MINIO_ROOT_PASSWORD: aegis_admin_password
    command: server /data --console-address ":9001"
```

### Compilar y Ejecutar el Servicio

El proyecto utiliza Maven (o Gradle, según convención interna) para el ciclo de construcción.

#### Con Maven:
1. Compilar y ejecutar pruebas unitarias/integración:
   ```bash
   ./mvnw clean verify
   ```
2. Ejecutar el microservicio localmente:
   ```bash
   ./mvnw spring-boot:run
   ```

#### Con Gradle:
1. Compilar y empaquetar el microservicio:
   ```bash
   ./gradlew clean build
   ```
2. Ejecutar el microservicio localmente:
   ```bash
   ./gradlew bootRun
   ```

El servicio estará disponible en `http://localhost:8080` (o el puerto configurado por defecto en `application.yml`).
