# Especificación de Requisitos - aegis-sign

Este documento detalla los requisitos funcionales, no funcionales y de integración necesarios para la implementación del microservicio crítico `aegis-sign`. Está diseñado para guiar el desarrollo de la arquitectura reactiva y asegurar el cumplimiento de estándares de seguridad y validez legal.

---

## 1. Requisitos Funcionales (RF)

### 1.1. Módulo KYC (Know Your Customer)

#### [x] RF-KYC-01: Gestión de Sesión Efímera (Implementado)
*   **Descripción:** El sistema debe permitir la creación de una sesión de verificación de identidad con un identificador único global (UUID) y un tiempo de vida (TTL) configurable.
*   **Detalles:**
    *   La sesión almacena el estado actual del flujo (e.g., `CREATED`, `DOCUMENT_UPLOADED`, `BIOMETRIC_COMPLETED`, `VERIFIED`, `FAILED`).
    *   El TTL inicial de la sesión en caché (Redis) es de 30 minutos por seguridad y optimización de recursos.
    *   *Estado:* Implementado a nivel de controlador, puerto de entrada (`KycUseCase`), interactores, y almacenamiento R2DBC/Redis.

#### [/] RF-KYC-02: Pipeline de Ingesta Documental (Parcialmente Implementado / Simplificado)
*   **Descripción:** El sistema debe aceptar la subida asíncrona de archivos multiparte que contengan imágenes del documento de identidad del usuario (anverso y reverso para identificaciones tipo tarjeta, o página principal para pasaportes).
*   **Detalles:**
    *   Formatos permitidos: `JPEG`, `PNG`, `PDF`.
    *   Tamaño máximo permitido: 10 MB.
    *   Almacenamiento inmediato en un bucket privado de MinIO temporal, asociado al UUID de la sesión KYC.
    *   *Estado:* Implementado a través de los endpoints dedicados `/api/v1/kyc/sessions/{id}/documents` y `/api/v1/kyc/sessions/{id}/biometrics`, los cuales suben el archivo a MinIO y asocian metadatos de forma diferenciada.

#### [x] RF-KYC-03: Procesamiento OCR y MRZ (Implementado)
*   **Descripción:** El sistema debe procesar el documento cargado para extraer campos de texto estructurados mediante reconocimiento óptico de caracteres (OCR) y verificar la validez de la zona de lectura mecánica (MRZ).
*   **Detalles:**
    *   Campos requeridos: Nombre completo, apellidos, número de documento, fecha de nacimiento, fecha de vencimiento, sexo y nacionalidad.
    *   Cálculo y validación de los dígitos de control (checksum) del bloque MRZ de acuerdo con el estándar ICAO Doc 9303.
    *   Integración local con **Tess4j (Tesseract)** para extracción sin APIs externas.
    *   *Estado:* Implementado y verificado con pruebas unitarias.

#### [/] RF-KYC-04: Captura Biométrica y Detección de Vida (Parcialmente Implementado)
*   **Descripción:** El sistema debe recibir un selfie fotográfico del usuario para el análisis biométrico de correspondencia.
*   **Detalles:**
    *   Validación activa de calidad de imagen: contraste, resolución (min 480x480) y detección de rostro.
    *   Detección de vida (Liveness) mediante análisis de varianza de frames (lógica simplificada).
    *   *Estado:* Lógica implementada en `BiometricValidationService`. Falta calibración con imágenes reales.

#### [/] RF-KYC-05: Comparación Facial (Facial Match 1:1) (Parcialmente Implementado)
*   **Descripción:** El sistema debe comparar la biometría facial del selfie con la fotografía extraída del documento de identidad cargado en la sesión.
*   **Detalles:**
    *   Cálculo de score de confianza basado en similitud estructural y vectorial.
    *   Umbral de aceptación configurable (default 80%).
    *   *Estado:* Lógica implementada en `BiometricMatchingService`. Pendiente de verificación con dataset real.

---

### 1.2. Módulo de Firma Electrónica Avanzada (FEA)

#### [x] RF-SIG-01: Renderizado Dinámico de Contratos (Implementado)
*   **Descripción:** El sistema debe ensamblar contratos digitales a partir de plantillas reutilizables y cláusulas modulares parametrizadas.
*   **Detalles:**
    *   Soporte para recibir un JSON estructurado con el ID de la plantilla y los valores de sustitución para las variables dinámicas del contrato.
    *   Generación reactiva del archivo PDF y su almacenamiento en MinIO (bucket definitivo).
    *   *Estado:* Implementado de forma real en `PdfTemplateCompiler` utilizando OpenPDF (análisis sintáctico y reemplazo de llaves dobles `{{variable}}`).

#### [x] RF-SIG-02: Hashing Criptográfico e Integridad (Implementado)
*   **Descripción:** El sistema debe calcular el resumen criptográfico (hash) del archivo PDF compilado antes de la firma.
*   **Detalles:**
    *   Algoritmo mandatorio: SHA-256.
    *   El hash debe ser devuelto al cliente y persistido en la base de datos PostgreSQL para garantizar el principio de no alterabilidad previa a la firma.
    *   *Estado:* Implementado y acoplado al flujo reactivo.

#### [x] RF-SIG-03: Sellado Digital PAdES (X.509 PKI) (Implementado)
*   **Descripción:** El sistema debe aplicar una firma digital basada en criptografía asimétrica sobre el PDF conforme al estándar PAdES (PDF Advanced Electronic Signatures).
*   **Detalles:**
    *   Firmado criptográfico real usando **BouncyCastle** (`SHA256withRSA`).
    *   Uso de certificados X.509 y llaves privadas gestionadas por el sistema.
    *   *Estado:* Implementado en `SignatureServiceAdapter`.

#### [/] RF-SIG-04: Compilación del Audit Trail (Pista de Auditoría) (Parcialmente Implementado)
*   **Descripción:** El sistema debe generar un registro técnico e inmutable que recopile toda la evidencia digital recopilada durante el proceso de KYC y firma.
*   **Detalles:**
    *   El Audit Trail debe consolidarse en un JSON estructurado y posteriormente en un reporte PDF inalterable.
    *   Debe incluir:
        *   UUID de la sesión KYC y resultados (Score biométrico, OCR).
        *   Hash SHA-256 del contrato antes y después del sellado.
        *   Metadatos de red del firmante: Dirección IP de origen, User-Agent, coordenadas geográficas (si están disponibles), y timestamp preciso (servidor local sincronizado NTP).
        *   Registro explícito del consentimiento (e.g., IP, Timestamp del clic "Aceptar").
    *   El PDF del Audit Trail se firmará digitalmente con el certificado raíz de la organización para asegurar su validez ante auditorías legales.
    *   *Estado:* Parcialmente implementado. El `SignatureInteractor` compila el evento y lo persiste en PostgreSQL a través de `AuditTrailRepositoryPort`, pero no se genera un PDF firmado independiente para el Audit Trail.

---

## 2. Requisitos No Funcionales (RNF)

### [x] RNF-01: Alta Concurrencia y Reactividad (Performance) (Implementado)
*   **Métrica:** El sistema debe procesar cargas masivas de archivos e interacciones concurrentes sin bloquear hilos.
*   **Implementación:**
    *   Uso de **Spring WebFlux** (Project Reactor) y Netty.
    *   Base de datos no bloqueante con **R2DBC** para PostgreSQL.
    *   Flujos binarios controlados con contrapresión (backpressure) para evitar el agotamiento de memoria física.

### [x] RNF-02: Aislamiento y Autohospedaje (Self-Hosted) (Implementado)
*   **Métrica:** Dependencia cero de servicios de nube de terceros para el cumplimiento de normativas de soberanía de datos.
*   **Implementación:**
    *   Tanto la base de datos (PostgreSQL), la caché (Redis), el almacenamiento de objetos (MinIO) como los motores de IA y OCR funcionan localmente dentro de contenedores o infraestructura privada.

### [/] RNF-03: Seguridad y Gestión de Llaves Criptográficas (Parcialmente Implementado)
*   **Métrica:** Protección estricta de las llaves privadas utilizadas para el firmado y cifrado de los datos confidenciales del KYC.
*   **Implementación:**
    *   Las llaves privadas se cargan de forma segura al arrancar la aplicación.
    *   Cifrado AES-256 en base de datos para datos sensibles (PII).
    *   *Estado:* La configuración de seguridad básica está implementada, pero falta integrar el Keystore real/HSM por software para firmar los PDFs.

### [x] RNF-04: Ciclo de Vida de Datos Sensibles (GDPR/Compliance) (Implementado)
*   **Métrica:** Reducción de la huella de datos biométricos para cumplir con la legislación de protección de datos personales.
*   **Implementación:**
    *   Purga de archivos biométricos crudos del almacenamiento de objetos temporal (MinIO) tras un máximo de 7 días naturales mediante el componente `StoragePurgeWorker`.
    *   Caché temporal de sesión KYC en Redis expira de forma automática.
    *   *Estado:* Implementado y verificado.

### [x] RNF-05: Portabilidad y Despliegue (Implementado)
*   **Métrica:** Preparado para entornos de orquestación de contenedores y microservicios modernos.
*   **Implementación:**
    *   Imagen Docker optimizada utilizando Buildpacks de Spring Boot.
    *   Preparado para compilación nativa AOT con GraalVM.

### [x] RNF-06: Compatibilidad de Entorno de Ejecución (Nuevo)
*   **Métrica:** El sistema debe ser capaz de compilar y ejecutar sus pruebas en entornos de Java modernos (Java 21 a 25).
*   **Implementación:**
    *   Configuración de `maven-surefire-plugin` con el flag `-Dnet.bytebuddy.experimental=true` para permitir el funcionamiento de Mockito en versiones preliminares de Java (como Java 25).

---

## 3. Requisitos de Integración y API

### [x] 3.1. Idempotencia en Transacciones (Implementado)
*   Los endpoints `/api/v1/signatures/prepare` y `/api/v1/signatures/sign` soportan cabeceras `Idempotency-Key` para evitar ejecuciones duplicadas de firma y renderizado.

### [x] 3.2. Rate Limiting reactivo (Implementado)
*   Rate limiting aplicado reactivamente en los endpoints públicos de KYC usando Redis (algoritmo Token Bucket).
*   Implementado mediante `TokenBucketRateLimiterFilter` y scripts Lua en Redis para atomicidad.
*   *Estado:* Implementado y verificado.

### [x] 3.3. Monitoreo e Indicadores (Observabilidad) (Implementado)
*   Endpoints `/actuator/prometheus` expuestos para monitoreo de salud y micrometer-tracing para rastreo distribuido.

---

## 4. Requisitos de Ingeniería de Software (Implementación Completa)

### [x] 4.1. Diseño y Modelado de Base de Datos (PostgreSQL) (Implementado)
*   **Estatus:** Tablas `kyc_sessions`, `contracts`, `signatures`, `audit_trails` modeladas con tipos de datos correctos (incluyendo JSONB) y claves UUIDv4. Índices creados para campos de alto rendimiento.

### [x] 4.2. Creación y Evolución de la Base de Datos (Migraciones) (Implementado)
*   **Estatus:** Migraciones manejadas mediante Flyway con el script `V1__init_schema.sql` mapeado en `resources/db/migration`.

### [x] 4.3. Diseño de Endpoints (API Specification) (Implementado)
*   **Estatus:** Documentación auto-generada expuesta mediante OpenAPI 3.0/Swagger Reactivo y estructura unificada de respuestas/errores (`ApiResponse` y RFC 7807).

### [x] 4.4. Implementación de Endpoints (Clean Architecture) (Implementado)
*   **Estatus:** Separación modular perfecta en capas de Dominio, Aplicación e Infraestructura, implementando controladores REST no bloqueantes y servicios reactivos.

### [/] 4.5. Estrategia de Testing (Aseguramiento de Calidad) (Parcialmente Probado)
*   **Estatus:**
    *   **Unit Tests:** 17 pruebas unitarias de servicios de dominio, casos de uso e interactores ejecutadas y superadas con éxito.
    *   **Architecture Tests:** Pruebas de cumplimiento de la arquitectura con ArchUnit ejecutadas y superadas con éxito.
    *   **Integration Tests:** Pruebas de integración E2E implementadas (`SignatureControllerIntegrationTest`), pero pendientes de ejecución debido a la falta de un entorno Docker activo local para Testcontainers.

---

### Context & Navigation
- [GEMINI.md](../GEMINI.md)
- [goal.md](goal.md)
- [status.md](status.md)
- [business-logic.md](business-logic.md)
