# Especificación de Requisitos - aegis-sign

Este documento detalla los requisitos funcionales, no funcionales y de integración necesarios para la implementación del microservicio crítico `aegis-sign`. Está diseñado para guiar el desarrollo de la arquitectura reactiva y asegurar el cumplimiento de estándares de seguridad y validez legal.

---

## 1. Requisitos Funcionales (RF)

### 1.1. Módulo KYC (Know Your Customer)

#### RF-KYC-01: Gestión de Sesión Efímera
*   **Descripción:** El sistema debe permitir la creación de una sesión de verificación de identidad con un identificador único global (UUID) y un tiempo de vida (TTL) configurable.
*   **Detalles:**
    *   La sesión almacenará el estado actual del flujo (e.g., `CREATED`, `DOCUMENT_UPLOADED`, `BIOMETRIC_COMPLETED`, `VERIFIED`, `FAILED`).
    *   El TTL inicial de la sesión en caché (Redis) debe ser de 30 minutos por seguridad y optimización de recursos.

#### RF-KYC-02: Pipeline de Ingesta Documental
*   **Descripción:** El sistema debe aceptar la subida asíncrona de archivos multiparte que contengan imágenes del documento de identidad del usuario (anverso y reverso para identificaciones tipo tarjeta, o página principal para pasaportes).
*   **Detalles:**
    *   Formatos permitidos: `JPEG`, `PNG`, `PDF`.
    *   Tamaño máximo permitido: 10 MB.
    *   Almacenamiento inmediato en un bucket privado de MinIO temporal, asociado al UUID de la sesión KYC.

#### RF-KYC-03: Procesamiento OCR y MRZ
*   **Descripción:** El sistema debe procesar el documento cargado para extraer campos de texto estructurados mediante reconocimiento óptico de caracteres (OCR) y verificar la validez de la zona de lectura mecánica (MRZ).
*   **Detalles:**
    *   Campos requeridos: Nombre completo, apellidos, número de documento, fecha de nacimiento, fecha de vencimiento, sexo y nacionalidad.
    *   Cálculo y validación de los dígitos de control (checksum) del bloque MRZ de acuerdo con el estándar ICAO Doc 9303.
    *   Si los checksums MRZ no son válidos, la sesión debe marcarse inmediatamente con un estado de alerta/fallo.

#### RF-KYC-04: Captura Biométrica y Detección de Vida
*   **Descripción:** El sistema debe recibir un selfie fotográfico del usuario para el análisis biométrico de correspondencia.
*   **Detalles:**
    *   El endpoint debe recibir la imagen en formato raw o base64 codificado dentro de una estructura JSON/Multipart.
    *   Debe ejecutarse una validación básica de calidad de imagen (contraste, resolución mínima y detección de un único rostro presente).

#### RF-KYC-05: Comparación Facial (Facial Match 1:1)
*   **Descripción:** El sistema debe comparar la biometría facial del selfie con la fotografía extraída del documento de identidad cargado en la sesión.
*   **Detalles:**
    *   La comparación debe realizarse a través de un motor local integrado (e.g., ONNX Runtime o TensorFlow Java) sin enviar datos a APIs externas.
    *   Debe retornar un score de confianza (porcentaje de similitud de características vectoriales).
    *   El umbral (threshold) de aceptación de coincidencia debe ser configurable mediante variables de entorno (e.g., mínimo 85%).

---

### 1.2. Módulo de Firma Electrónica Avanzada (FEA)

#### RF-SIG-01: Renderizado Dinámico de Contratos
*   **Descripción:** El sistema debe ensamblar contratos digitales a partir de plantillas reutilizables y cláusulas modulares parametrizadas.
*   **Detalles:**
    *   Soporte para recibir un JSON estructurado con el ID de la plantilla y los valores de sustitución para las variables dinámicas del contrato.
    *   Generación reactiva del archivo PDF y su almacenamiento en MinIO (bucket definitivo).

#### RF-SIG-02: Hashing Criptográfico e Integridad
*   **Descripción:** El sistema debe calcular el resumen criptográfico (hash) del archivo PDF compilado antes de la firma.
*   **Detalles:**
    *   Algoritmo mandatorio: SHA-256.
    *   El hash debe ser devuelto al cliente y persistido en la base de datos PostgreSQL para garantizar el principio de no alterabilidad previa a la firma.

#### RF-SIG-03: Sellado Digital PAdES (X.509 PKI)
*   **Descripción:** El sistema debe aplicar una firma digital basada en criptografía asimétrica sobre el PDF conforme al estándar PAdES (PDF Advanced Electronic Signatures).
*   **Detalles:**
    *   El firmado utilizará llaves privadas y certificados X.509 gestionados por una PKI (Public Key Infrastructure) interna.
    *   La llave privada del firmante (o del sistema en representación del proceso de firma delegada) se almacenará en un almacén de llaves seguro (KeyStore/HSM simulado por software).
    *   El certificado X.509 debe incluir metadatos de identidad del firmante y sello de tiempo de la firma.

#### RF-SIG-04: Compilación del Audit Trail (Pista de Auditoría)
*   **Descripción:** El sistema debe generar un registro técnico e inmutable que recopile toda la evidencia digital recopilada durante el proceso de KYC y firma.
*   **Detalles:**
    *   El Audit Trail debe consolidarse en un JSON estructurado y posteriormente en un reporte PDF inalterable.
    *   Debe incluir:
        *   UUID de la sesión KYC y resultados (Score biométrico, OCR).
        *   Hash SHA-256 del contrato antes y después del sellado.
        *   Metadatos de red del firmante: Dirección IP de origen, User-Agent, coordenadas geográficas (si están disponibles), y timestamp preciso (servidor local sincronizado NTP).
        *   Registro explícito del consentimiento (e.g., IP, Timestamp del clic "Aceptar").
    *   El PDF del Audit Trail se firmará digitalmente con el certificado raíz de la organización para asegurar su validez ante auditorías legales.

---

## 2. Requisitos No Funcionales (RNF)

### RNF-01: Alta Concurrencia y Reactividad (Performance)
*   **Métrica:** El sistema debe procesar cargas masivas de archivos e interacciones concurrentes sin bloquear hilos.
*   **Implementación:**
    *   Uso mandatorio de **Spring WebFlux** (Project Reactor) y Netty.
    *   Evitar bloqueos de E/S de base de datos utilizando controladores reactivos (**R2DBC** en lugar de JDBC para PostgreSQL).
    *   Flujos binarios controlados con contrapresión (backpressure) para evitar el agotamiento de memoria física ante archivos de gran tamaño.

### RNF-02: Aislamiento y Autohospedaje (Self-Hosted)
*   **Métrica:** Dependencia cero de servicios de nube de terceros para el cumplimiento de normativas de soberanía de datos.
*   **Implementación:**
    *   Tanto la base de datos (PostgreSQL), la caché (Redis), el almacenamiento de objetos (MinIO) como los modelos de IA (OCR y biometría) deben ejecutarse en infraestructura local o contenedores autohospedados de la red privada.

### RNF-03: Seguridad y Gestión de Llaves Criptográficas
*   **Métrica:** Protección estricta de las llaves privadas utilizadas para el firmado y cifrado de los datos confidenciales del KYC.
*   **Implementación:**
    *   Las llaves privadas de firma deben ser cargadas de manera segura en memoria a través de configuraciones inyectadas en tiempo de despliegue (Secrets de Kubernetes o variables cifradas, nunca en texto plano en código fuente).
    *   Cifrado de datos en reposo (AES-256) en la base de datos para campos de identificación personal (PII).

### RNF-04: Ciclo de Vida de Datos Sensibles (GDPR/Compliance)
*   **Métrica:** Reducción de la huella de datos biométricos para cumplir con la legislación de protección de datos personales.
*   **Implementación:**
    *   Los archivos biométricos crudos (selfies y fotos de documentos) deben ser eliminados del almacenamiento de objetos temporal (MinIO) tras un máximo de 7 días naturales una vez finalizado y aprobado el Audit Trail.
    *   La caché temporal en Redis debe expirar inmediatamente al cerrar la sesión KYC.

### RNF-05: Portabilidad y Despliegue
*   **Métrica:** Preparado para entornos de orquestación de contenedores y microservicios modernos.
*   **Implementación:**
    *   Generación de imágenes Docker optimizadas utilizando Buildpacks de Spring Boot.
    *   Arranque y consumo mínimo de recursos mediante compatibilidad con compilación nativa AOT (Ahead-Of-Time) mediante GraalVM.

---

## 3. Requisitos de Integración y API

### 3.1. Idempotencia en Transacciones
*   Los endpoints `/api/v1/signatures/prepare` y `/api/v1/signatures/sign` deben soportar cabeceras de idempotencia (`Idempotency-Key`) para prevenir el firmado doble o el renderizado duplicado en caso de reintentos por fallos de red.

### 3.2. Rate Limiting reactivo
*   Protección contra denegación de servicio (DoS) en el pipeline de KYC mediante un filtro reactivo en Spring WebFlux que aplique límites de peticiones por dirección IP utilizando la base de datos en memoria Redis (algoritmo Token Bucket).

### 3.3. Monitoreo e Indicadores (Observabilidad)
*   Implementación de métricas de rendimiento y salud mediante Spring Boot Actuator expuestas en formato compatible con Prometheus (`/actuator/prometheus`).
*   Trazado distribuido mediante micrometer-tracing para rastrear el flujo de una petición de firma a través de los adaptadores de infraestructura.

---

## 4. Requisitos de Ingeniería de Software (Implementación Completa)

### 4.1. Diseño y Modelado de Base de Datos (PostgreSQL)
*   **Descripción:** El modelo relacional debe estructurarse para soportar la trazabilidad exigida por las auditorías (Audit Trail) y la concurrencia reactiva.
*   **Requisitos Estructurales:**
    *   **`kyc_sessions`:** Control de estado. Campos clave: `id` (UUIDv4), `status`, `expires_at`, `extracted_data` (JSONB para los datos del OCR), `biometric_score`.
    *   **`contracts`:** Gestión del documento. Campos clave: `id` (UUIDv4), `template_id`, `status`, `document_hash_sha256`, `minio_uri`, `created_at`.
    *   **`signatures`:** Registro de la acción de firma. Campos clave: `id` (UUIDv4), `contract_id`, `signer_info` (JSONB), `x509_certificate_sn`, `timestamp`.
    *   **`audit_trails`:** Evidencia legal final. Campos clave: `id` (UUIDv4), `contract_id`, `kyc_session_id`, `trail_manifest` (JSONB con IPs, User-Agents, hashes previos), `final_signed_pdf_uri`.
*   **Tipos de Datos y Restricciones:** Uso extensivo de `JSONB` para estructuras de datos dinámicas (cláusulas, metadatos variables). Claves primarias siempre basadas en UUID para evitar fuga de información secuencial.

### 4.2. Creación y Evolución de la Base de Datos (Migraciones)
*   **Descripción:** La base de datos debe estar versionada estrictamente, prohibiendo la generación automática de esquemas por parte del ORM (ej. `hibernate.hbm2ddl.auto=none`).
*   **Requisitos:**
    *   Uso de **Flyway** o **Liquibase** como herramienta de control de versiones de base de datos.
    *   Los scripts SQL (ej. `V1__init_schema.sql`, `V2__add_indexes_audit.sql`) deben estar en `src/main/resources/db/migration`.
    *   Las migraciones deben ejecutarse de forma automática en el arranque de la aplicación para entornos locales/desarrollo (integración con Spring Boot).

### 4.3. Diseño de Endpoints (API Specification)
*   **Descripción:** La interfaz RESTful debe ser predecible, auto-documentada y manejar los errores de forma uniforme.
*   **Requisitos:**
    *   **Especificación:** Documentación generada automáticamente mediante **OpenAPI 3.0** (`springdoc-openapi-webflux-ui`).
    *   **Estandarización de Respuestas:** Todos los endpoints deben retornar una envoltura genérica (e.g., `ApiResponse<T>`) garantizando que los clientes consuman una estructura unificada para éxitos y fallos.
    *   **Manejo de Errores Global:** Implementación del estándar **RFC 7807 (Problem Details for HTTP APIs)** utilizando un `@ControllerAdvice` reactivo para mapear excepciones de dominio a respuestas HTTP correctas (400, 404, 409, 500).

### 4.4. Implementación de Endpoints (Clean Architecture)
*   **Descripción:** La codificación de la API debe respetar estrictamente las reglas de la Arquitectura Hexagonal y la propagación reactiva.
*   **Requisitos:**
    *   **Capa Input Adapter (Controllers):** Controladores REST Reactivos (`@RestController` + `Mono`/`Flux`). Validación estricta de entrada usando DTOs y `jakarta.validation`.
    *   **Capa Application (Use Cases):** Los servicios deben orquestar la lógica de negocio consumiendo los puertos de entrada (Input Ports). No deben conocer detalles de HTTP (Request/Response) ni de SQL.
    *   **Capa Domain (Entidades):** Objetos de negocio puros, libres de dependencias de Spring (`@Component`, `@Service`) o de acceso a datos (`@Table`, `@Column`).
    *   **Capa Output Adapter (Infraestructura):** Implementaciones reactivas para R2DBC, Redis y MinIO, encapsulando las conversiones entre entidades de dominio y modelos de persistencia.

### 4.5. Estrategia de Testing (Aseguramiento de Calidad)
*   **Descripción:** El sistema debe contar con pruebas automatizadas rigurosas en los distintos niveles de la pirámide de pruebas.
*   **Requisitos:**
    *   **Pruebas Unitarias (Lógica de Negocio):**
        *   **Frameworks:** JUnit 5 + Mockito + AssertJ.
        *   **Cobertura:** Mínimo del 85% para las capas de Dominio y Aplicación.
        *   **Validación Reactiva:** Uso intensivo de `StepVerifier` de `reactor-test` para evaluar comportamientos asíncronos y backpressure.
    *   **Pruebas de Integración (Adaptadores y API):**
        *   **Infraestructura Efímera:** Uso obligatorio de **Testcontainers** para levantar contenedores Docker reales (PostgreSQL, Redis, MinIO) durante las pruebas, descartando el uso de H2 en memoria para asegurar paridad total con producción.
        *   **Endpoints:** Pruebas E2E de los controladores utilizando `WebTestClient`.
    *   **Pruebas de Arquitectura (Fitness Functions):**
        *   Uso de **ArchUnit** para verificar mediante pruebas automatizadas que las reglas de la Arquitectura Hexagonal no se rompan (ej. "La capa de Dominio no puede depender de la capa de Infraestructura").
