# Database Model & Data Access (EXHAUSTIVE)

## Data Access Layer
- **Framework/Technology**: R2DBC (Reactive Relational Database Connectivity) / Spring Data R2DBC.
- **Base Directory/Package**: `src/main/java/com/aegis/sign/infrastructure/adapter/output`
- **Connection Management**: Connection Pool managed by Spring Boot, configured via `application.yml`.

## Entity Relationship Diagram (ERD) / Schema Map
```mermaid
erDiagram
    TEMPLATES ||--o{ CLAUSES : "contains"
    TEMPLATES ||--o{ CONTRACTS : "instantiates"
    CONTRACTS ||--o{ SIGNATURES : "has"
    CONTRACTS ||--|| AUDIT_TRAILS : "has"
    KYC_DOCUMENTS ||--|| SIGNATURES : "verifies_signer"
    
    TEMPLATES {
        uuid id PK
        string name
        integer version
        jsonb layout_metadata
    }

    CLAUSES {
        uuid id PK
        uuid template_id FK
        text content
        jsonb variables
    }

    CONTRACTS {
        uuid id PK
        uuid template_id FK
        string status
        string content_hash
        string minio_uri
        timestamp created_at
    }
    
    SIGNATURES {
        uuid id PK
        uuid contract_id FK
        string signer_id
        timestamp signed_at
        string signature_value
        string cert_thumbprint
    }
    
    AUDIT_TRAILS {
        uuid id PK
        uuid contract_id FK
        jsonb events
        timestamp closed_at
    }

    KYC_DOCUMENTS {
        uuid id PK
        string signer_id UK "Unique ID to link with signatures"
        string first_name
        string last_name
        string document_number
        date birth_date
        string mrz_data
        string face_match_score
    }
```

## Complete Data Structure Inventory
| Name (Table/Coll) | Description / Purpose | Key Fields (PK/FK/Index) | Related Entities | Business Object / Model |
|-------------------|-----------------------|--------------------------|------------------|-------------------------|
| templates         | Contract blueprints.  | PK: id                   | clauses, contracts| Template |
| clauses           | Reusable legal text.  | PK: id, FK: template_id  | templates        | Clause |
| contracts         | Stores metadata and status of contracts. | PK: id, FK: template_id | signatures, audit_trails | Contract |
| signatures        | Individual signatures applied to contracts. | PK: id, FK: contract_id | contracts, kyc_documents | Signature |
| audit_trails      | Immutable logs for legal compliance. | PK: id, FK: contract_id | contracts | AuditTrail |
| kyc_documents     | Validated identity data. | PK: id, UK: signer_id    | signatures       | KycDocument |
| kyc_sessions (Redis) | Temporary session state. | Key: session_id | - | KycSession |
| otps (Redis)      | Temporary OTP codes for consent. | Key: signature_id | - | Otp |

## Relationships and Data Integrity
- **Constraints/Relationships**: 
    - `clauses` -> `templates` (Many-to-One).
    - `contracts` -> `templates` (Many-to-One).
    - `signatures` -> `contracts` (Many-to-One).
    - `audit_trails` -> `contracts` (One-to-One).
    - `kyc_documents.signer_id` matches `signatures.signer_id`.
- **Logic in DB/Storage**: Use of `jsonb` in PostgreSQL for flexible but queryable audit events and clause variables.
- **Identity Generation**: UUIDs (v4) for all primary keys.

## Critical Query Patterns
1. **Contract Assembly**: Joining `templates` and `clauses` to render the PDF.
2. **Audit Trail Retrieval**: Fetching all events for a specific contract ID.
3. **Signer Validation**: Verifying if `signer_id` in a signature request has a matching approved `kyc_documents` entry.

---

### Context & Navigation
- [GEMINI.md](../GEMINI.md)
- [architecture.md](architecture.md)
- [business_logic.md](business_logic.md)
