# Reporte de Estado del Proyecto: aegis-sign
> Actualizado: 2026-06-24 — verificado contra código fuente (ver `docs/history/` para snapshots previos)

## 1. Resumen Ejecutivo
El proyecto `aegis-sign` es un microservicio robusto diseñado para la verificación de identidad (KYC) y firma electrónica. Utiliza una **Arquitectura Hexagonal** con un enfoque **Reactivo (Spring WebFlux + R2DBC)**, lo que garantiza alta concurrencia y nula dependencia de APIs externas (autohospedaje).

Se ha realizado una auditoría estricta de la estructura de código y se verificó el cumplimiento de las capas, así como el funcionamiento de las reglas de negocio críticas.

---

## 2. Estado de Funcionalidades e Indicadores Clave

### 2.1. Tareas del Plan Core Finalizadas
- **SHA-256 Document Hashing**: Implementado para garantizar la inmutabilidad de los contratos antes de la firma.
- **X.509 Digital Sealing (BouncyCastle)**: Firma digital asimétrica real integrada para el sellado de documentos.
- **Real OCR Integration (Tess4j)**: Procesamiento local de documentos de identidad sin dependencias de nube.
- **MRZ Validation**: Motor de validación de checksums (ICAO Doc 9303) para TD1, TD2 y TD3.
- **Biometric Matching and Liveness**: Comparación facial 1:1 (`BiometricMatchingService`) conectada en `KycInteractor.submitBiometrics` desde 2026-06-24; detección de vida y calidad de imagen (`BiometricValidationService`) operativas. Pendiente calibración del `matchThreshold` con dataset real.
- **Storage Purge Worker (GDPR)**: Limpieza automática de datos personales tras 7 días.

### 2.2. Matriz de Cumplimiento de Requisitos

| ID | Requisito | Estatus | Comentarios |
|----|-----------|---------|-------------|
| **RF-KYC-01** | Gestión de Sesión Efímera | **Completado** | Implementado con Redis (TTL) y R2DBC. |
| **RF-KYC-02** | Pipeline de Ingesta Documental | **Completado** | Carga multipart no bloqueante a MinIO. |
| **RF-KYC-03** | Procesamiento OCR y MRZ | **Completado** | Integración real con Tess4j y validación MRZ. |
| **RF-KYC-04** | Captura Biométrica y Calidad | **Completado** | Análisis de calidad y detección de rostro implementado. |
| **RF-KYC-05** | Comparación Facial 1:1 | **Completado** | `BiometricMatchingService` conectado en `submitBiometrics`: descarga la imagen del documento (guardada en `submitIdDocument`), compara contra el selfie y rechaza la sesión (`BIOMETRIC_FAILED` / `FACE_MATCH_FAILED`) si el score no supera el umbral. Pendiente calibrar el umbral con dataset real. |
| **RF-SIG-01** | Renderizado de Contratos | **Completado** | Compilación reactiva en PDF vía plantillas JSON. |
| **RF-SIG-02** | Hashing e Integridad | **Completado** | Generación de SHA-256 antes del sellado. |
| **RF-SIG-03** | Sellado Digital PAdES | **Completado** | Firma criptográfica real con BouncyCastle. |
| **RF-SIG-04** | Compilación de Audit Trail | **Completado** | Evidencias estructuradas en JSON y BD (PDF final en backlog). |
| **RNF-02** | Aislamiento y Autohospedaje | **Completado** | Nula dependencia de APIs externas. |
| **3.2** | Rate Limiting reactivo | **Completado** | Implementado vía script Lua en Redis. |

---

## 3. Análisis Técnico y de Seguridad

### 🏗️ Arquitectura y Calidad
- **Puntos Fuertes**: Excelente separación de conceptos. El dominio es puro y reactivo.
- **Pruebas**: 80 pruebas (unitarias + arquitectura ArchUnit) superadas (`mvn test -Djacoco.skip=true`, verificado 2026-06-24). Pruebas de integración E2E estructuradas, pendientes de Docker/Testcontainers para ejecutarse.
- **Entorno**: Compatible con Java 21 a 25 gracias al flag experimental de ByteBuddy para Mockito.

### 🛡️ Seguridad
- **Implementado**: Hashing SHA-256, firmas digitales reales, limpieza de PII (GDPR), Rate Limiting.
- **Mejora Crítica**: La gestión de secretos debe moverse de `application.yml` a un Secret Manager externo para entornos de producción.
- **Persistencia**: Se recomienda el cifrado de datos sensibles (`extractedData`) en la base de datos (At-Rest Encryption).

---

## 4. Gaps y Próximos Pasos

### 🛠️ Pendiente de Implementación (Backlog)
1. **Persistencia de KeyStore Real**: Configurar la carga de certificados institucionales desde archivos `.p12` persistentes (actualmente en memoria).
2. **Informe de Audit Trail Final en PDF**: Generar el documento sellado inalterable que consolide todas las evidencias legales al final de la transacción.
3. **Calibración Biométrica**: Ajustar el `matchThreshold` (actualmente con modelo ONNX real si está disponible, o fallback simulado) mediante pruebas con un dataset real de imágenes de diversas calidades.
4. **Observabilidad**: Integrar métricas con Prometheus y traza distribuida para monitoreo en tiempo real.
5. **Modelo ONNX de embeddings faciales**: `BiometricMatchingService` usa similitud simulada (`calculateMockSimilarity`) si no encuentra el modelo en `biometrics.model-path`; falta empaquetar/desplegar un modelo real de producción.

### 🧪 Verificación de Entorno
- Las pruebas de integración requieren un entorno Docker activo para Testcontainers. En su defecto, se han validado las capas mediante mocks exhaustivos.
