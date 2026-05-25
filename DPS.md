# Documento de Plan de Pruebas de Sistema (DPS) - aegis-sign

Este documento describe el listado de pruebas específicas para verificar la correcto funcionamiento y conformidad técnica del microservicio `aegis-sign`. Incluye pruebas de nivel unitario, de integración, de API (flujos REST E2E) y de robustez no funcional.

---

## 1. Pruebas Unitarias y de Componentes (Automáticas)

Estas pruebas se pueden ejecutar a nivel de desarrollo ejecutando:
```bash
mvn test
```

### 1.1. Verificación del Hashing SHA-256 (`PdfTemplateCompilerTest`)
- **Objetivo:** Comprobar que el compilador calcula un resumen SHA-256 exacto.
- **Clase de Prueba:** [PdfTemplateCompilerTest.java](file:///c:/Users/JoseSanchez3/OneDrive%20-%20EPAM/Documents/workspace_joselu/aegis-sign/src/test/java/com/aegis/sign/domain/service/PdfTemplateCompilerTest.java)
- **Casos:**
  - `testCalculateHash()`: Pasa la cadena `"test content"` y valida que devuelva el hash hexadecimal `"6ae8a75555209fd6c44157c0aed8016e763ff435a19cf186f76863140143ff72"`.

### 1.2. Verificación del Sellado Digital PAdES (`SignatureServiceAdapterTest`)
- **Objetivo:** Comprobar el correcto cifrado y codificación asimétrica de la firma digital con BouncyCastle.
- **Clase de Prueba:** [SignatureServiceAdapterTest.java](file:///c:/Users/JoseSanchez3/OneDrive%20-%20EPAM/Documents/workspace_joselu/aegis-sign/src/test/java/com/aegis/sign/infrastructure/adapter/signature/SignatureServiceAdapterTest.java)
- **Casos:**
  - `signShouldReturnValidBase64String()`: Valida que la firma devuelta no sea nula, no empiece con el prefijo dummy `"signed-"` y sea un formato Base64 sintácticamente correcto.
  - `signShouldReturnDifferentSignaturesForDifferentHashes()`: Valida que al firmar dos resúmenes distintos con la misma clave privada, los Base64 de firma resultantes sean diferentes.

### 1.3. Verificación de Integración de OCR local con Tess4j (`OcrExtractorServiceTest`)
- **Objetivo:** Verificar que el motor de OCR Tesseract decodifique y extraiga con expresiones regulares los campos obligatorios del documento.
- **Clase de Prueba:** [OcrExtractorServiceTest.java](file:///c:/Users/JoseSanchez3/OneDrive%20-%20EPAM/Documents/workspace_joselu/aegis-sign/src/test/java/com/aegis/sign/domain/service/OcrExtractorServiceTest.java)
- **Casos:**
  - `shouldExtractDataUsingTesseract()`: Valida con un mock de `ITesseract` que al pasar un array de bytes de una imagen GIF válida, se extraiga el número de documento, fecha de nacimiento y fecha de caducidad.
  - `shouldFallbackToMockDataOnTesseractException()`: Valida que ante un error del motor de Tesseract (ej. falta de modelo de idioma `.traineddata`), el servicio no se caiga y devuelva los valores de fallback regulados para el negocio.
  - `shouldFallbackOnInvalidImage()`: Verifica el comportamiento del fallback al enviar bytes de imagen corruptos.

---

## 2. Pruebas de Flujo REST y API E2E (Integración)

Una vez que el servicio esté levantado en local o staging (URL base: `http://localhost:8080`), se deben ejecutar secuencialmente las siguientes peticiones HTTP para comprobar el ciclo de vida completo de verificación de identidad y firma de contrato.

### Paso 2.1: Crear una sesión KYC temporal
- **Objetivo:** Inicializar la sesión del firmante.
- **Petición (curl):**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/kyc/sessions?signerId=usr_789324" \
       -H "Accept: application/json"
  ```
- **Resultado Esperado (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "status": "PENDING",
      "signerId": "usr_789324",
      "documentMetadata": {}
    }
  }
  ```
- **Validar en DB:** El registro debe crearse en la tabla `kyc_sessions` con estado `'CREATED'`.

### Paso 2.2: Subir el documento de identidad (Anverso/Reverso)
- **Objetivo:** Simular la ingesta documental y procesado OCR/MRZ de la tarjeta de identificación.
- **Petición (curl):**
  *(Reemplazar `{session_id}` con el UUID devuelto en el Paso 2.1 e incorporar una imagen de prueba real/dummy).*
  ```bash
  curl -X POST "http://localhost:8080/api/v1/kyc/sessions/{session_id}/documents" \
       -F "file=@/path/to/test-document-id.png"
  ```
- **Resultado Esperado (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "{session_id}",
      "status": "PENDING",
      "documentMetadata": {
        "ID_DOCUMENT": "UPLOADED"
      }
    }
  }
  ```
- **Validación Interna:** El documento físico se debe almacenar en el bucket de MinIO temporal y registrar en la base de datos.

### Paso 2.3: Subir selfie del usuario (Biometría)
- **Objetivo:** Subir la imagen de correspondencia facial.
- **Petición (curl):**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/kyc/sessions/{session_id}/biometrics" \
       -F "file=@/path/to/selfie.jpg"
  ```
- **Resultado Esperado (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "{session_id}",
      "status": "PENDING",
      "documentMetadata": {
        "ID_DOCUMENT": "UPLOADED",
        "BIOMETRICS": "UPLOADED"
      }
    }
  }
  ```

### Paso 2.4: Verificar/Aprobar la sesión KYC
- **Objetivo:** Forzar la validación de la sesión para comprobar la transición del flujo y cálculo de biométrica.
- **Petición (curl):**
  ```bash
  curl -X GET "http://localhost:8080/api/v1/kyc/sessions/{session_id}"
  ```
- **Resultado Esperado (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "{session_id}",
      "status": "APPROVED",
      "signerId": "usr_789324"
    }
  }
  ```

### Paso 2.5: Preparar hash del contrato pre-firma
- **Objetivo:** Calcular y persistir el resumen SHA-256 del contrato antes del sellado.
- **Petición (curl):**
  *(Se debe usar un ID de contrato ya sembrado en base de datos).*
  ```bash
  curl -X POST "http://localhost:8080/api/v1/signatures/prepare?contractId={contract_id}"
  ```
- **Resultado Esperado (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": "a3589b2b489ef08b98218e8d5e89d1b0928a9b2b73fa3528b98fbc18cdd7e89a"
  }
  ```

### Paso 2.6: Firmar el contrato con sellado digital
- **Objetivo:** Ejecutar la firma digital institucional PAdES combinando la verificación de KYC previa.
- **Petición (curl):**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/signatures/sign" \
       -H "Content-Type: application/json" \
       -H "X-Forwarded-For: 198.51.100.42" \
       -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)" \
       -d '{
         "contractId": "{contract_id}",
         "kycSessionId": "{session_id}",
         "signerId": "usr_789324",
         "certificateThumbprint": "098F6BCD4621D373CADE4E832627B4F6"
       }'
  ```
- **Resultado Esperado (HTTP 200 OK):**
  Devuelve la entidad `Signature` conteniendo el resumen digital firmado en Base64 real.
  ```json
  {
    "success": true,
    "data": {
      "id": "8f3b145a-c93d-4952-b131-7b0b6910aef5",
      "contractId": "{contract_id}",
      "signerId": "usr_789324",
      "hash": "MIIEvwYJKoZIhvcNAQcCoIIEsDCCBKwCAQEx...",
      "certificateThumbprint": "098F6BCD4621D373CADE4E832627B4F6",
      "timestamp": "2026-05-26T00:45:12.356"
    }
  }
  ```

---

## 3. Pruebas de Robustez y Casos Límite (Edge Cases)

### 3.1. Prueba de Rate Limiting
- **Objetivo:** Validar que un cliente malicioso no sature el flujo de KYC.
- **Acción:** Ejecutar más de 10 peticiones por segundo en el endpoint `/api/v1/kyc/sessions` usando herramientas como `ab` (Apache Benchmark) o un bucle `for` rápido en consola.
  ```bash
  for i in {1..15}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/kyc/sessions?signerId=usr_limit; done
  ```
- **Resultado Esperado:** Las primeras 10 peticiones devolverán `200`, a partir de la 11ª se obtendrá `429` (Too Many Requests).

### 3.2. Prueba de Idempotencia en Firma
- **Objetivo:** Evitar la duplicidad de firmas por fallos de red.
- **Acción:** Enviar la petición de firma del Paso 2.6 incorporando la cabecera `Idempotency-Key: idemp-key-10023` dos veces seguidas.
- **Resultado Esperado:** Ambas llamadas deben retornar el mismo HTTP status `200` y el mismo cuerpo de respuesta. Internamente, la firma no debe computarse por duplicado.

### 3.3. Prueba de Expiración y Limpieza (GDPR)
- **Objetivo:** Garantizar la purga automática de PII (datos personales biométricos).
- **Acción:**
  1. Insertar manualmente un objeto temporal en MinIO simulando una carga de hace 8 días.
  2. Forzar o esperar la ejecución del cron `StoragePurgeWorker` (programado a las 2:00 AM o configurado con cron para pruebas rápidas).
  3. Comprobar que el archivo se ha eliminado de MinIO y la sesión asociada registra la eliminación.

### 3.4. Carga de Documento Corrupto / No Válido
- **Objetivo:** Proteger el motor OCR contra bloqueos por ficheros corruptos.
- **Acción:** Enviar un fichero de texto simple plano simulando ser una imagen en el endpoint de subida de documento.
- **Resultado Esperado:** El sistema debe capturar el error en [OcrExtractorService.java](file:///c:/Users/JoseSanchez3/OneDrive%20-%20EPAM/Documents/workspace_joselu/aegis-sign/src/main/java/com/aegis/sign/domain/service/OcrExtractorService.java), loguear el error y aplicar datos de fallback válidos para continuar sin lanzar un HTTP 500.
