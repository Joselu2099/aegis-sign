# GOAL.md - Directrices de Arquitectura e Implementación
## Microservicio Unificado de Identidad (KYC) y Firma Electrónica Autohospedado

### 1. Objetivo General del Proyecto
El objetivo principal de este proyecto es diseñar e implementar un microservicio crítico de nivel empresarial que centralice el proceso de **Verificación de Identidad (KYC - Know Your Customer)** y la **Gestión de Firma Electrónica Avanzada**, eliminando por completo la dependencia de proveedores externos (como DocuSign, Adobe Sign, Sumsub o Veriff). 

Este componente formará parte fundamental del ecosistema de la aplicación de contratos inteligentes de alquiler, garantizando la validez legal, la seguridad criptográfica y la inmutabilidad de la auditoría de cada contrato antes de su despliegue en la blockchain. Debe ser capaz de soportar alta demanda en tiempo real, garantizando latencias mínimas mediante un procesamiento asíncrono y optimizado.

---

### 2. Principios de Diseño y Filosofía "Zero-Third-Party"
* **Independencia Tecnológica:** Cada funcionalidad core (OCR, biometría facial, generación de hashes criptográficos y sellado documental) debe ser procesada de forma interna utilizando librerías open-source maduras o desarrollo propio.
* **Privacidad por Diseño (Privacy by Design):** Los datos biométricos y documentos de identidad son altamente sensibles. El microservicio debe encriptar la información tanto en tránsito (TLS 1.3) como en reposo (AES-256), aplicando políticas estrictas de purga de datos una vez validado el proceso.
* **Alta Concurrencia y Escalabilidad:** Dado que este servicio será altamente demandado durante las fases críticas del flujo de usuario, la arquitectura interna debe ser no bloqueante y orientada a eventos.
* **Cumplimiento Estándar (eIDAS / RGPD):** El flujo de firma debe replicar las mejores prácticas de los proveedores líderes para cumplir con los requisitos de la Firma Electrónica Avanzada (vínculo unívoco al firmante, detección de modificaciones posteriores y control exclusivo).

---

### 3. Alcance Funcional Detallado

#### A. Módulo KYC (Know Your Customer) Autohospedado
1. **Ingesta y Procesamiento de Documentos:**
   * Recepción de imágenes/PDFs de documentos de identidad (DNI, Pasaporte).
   * Implementación de un pipeline de OCR (ej. Tesseract, Apache PDFBox) para extraer campos clave (Nombre, Apellidos, Fecha de Nacimiento, Número de Documento).
2. **Validación Biométrica (Liveness Detection):**
   * Comparación del rostro extraído del documento con una selfie o ráfaga de fotos enviadas por el usuario en tiempo real.
   * Algoritmo básico de detección de vida (anti-spoofing) para evitar fraudes mediante fotos de pantallas o papel.
3. **Motor de Decisión (KYC Lifecycle):**
   * Estados de validación estrictos: `PENDING_DOCUMENTS`, `PROCESSING`, `MANUAL_REVIEW`, `APPROVED`, `REJECTED`.

#### B. Módulo de Firma Electrónica Avanzada
1. **Ensamblado Dinámico del Documento:**
   * Recibir la estructura JSON modular de cláusulas aprobadas y renderizar un PDF final inmutable (utilizando motores de plantillas eficientes).
2. **Generación del Hash Criptográfico:**
   * Cálculo del SHA-256 del documento final. Este hash actuará como la "huella digital" del contrato.
3. **Flujo de Consentimiento y Firma de las Partes:**
   * Captura del evento de firma explícita (OTP enviado al móvil/email o clave privada del usuario si aplica).
   * Sellado criptográfico del PDF utilizando un certificado digital interno de la plataforma (PKI propia) que garantice que el documento no ha sido modificado desde la firma.
4. **Pista de Auditoría Inmutable (Audit Trail):**
   * Generación de un manifiesto XML/JSON adjunto o embebido que registre IPs, marcas de tiempo de alta precisión (NTP), hashes de los documentos de identidad validados en el KYC y firmas electrónicas.

---

### 4. Stack Tecnológico Sugerido y Restricciones Técnicas
* **Lenguaje Base:** Java 21 / Spring Boot 3.x.
* **Paradigma de Concurrencia:** Spring WebFlux (Reactivo) para los endpoints de alta demanda y estados intermedios.
* **Persistencia Transaccional:** PostgreSQL (para auditoría, metadatos de documentos y estados del ciclo de vida).
* **Caché y Mensajería Interna:** Redis (para control de sesiones KYC temporales y tasas de rate-limiting).
* **Almacenamiento de Archivos:** MinIO (Alternativa Open-Source auto-hospedada compatible con la API de AWS S3) con cifrado del lado del servidor.
* **Procesamiento de Imágenes y Bio-Core:** OpenCV / DeepFace o integraciones nativas de bajo nivel encapsuladas para evitar fugas de memoria.

---

### 5. Estrategia de Implementación por Fases

* **Fase 1 (Capa de Datos y Documentos):** Configuración de la base de datos, almacenamiento en MinIO y motor de renderizado de PDFs a partir de módulos.
* **Fase 2 (Core Criptográfico de Firma):** Implementación del hashing SHA-256, lógica de sellado digital con certificados X.509 y generación del Audit Trail.
* **Fase 3 (Core KYC):** Integración de los componentes de extracción OCR y validación facial estática.
* **Fase 4 (Orquestación y Eventos):** Exposición de la API REST limpia hacia el API Gateway y publicación de eventos en el bróker central de la aplicación (ej. `KycApproved`, `ContractFullySigned`).