-- V1__init_schema.sql
-- Initial schema for Aegis Sign microservice
-- Handles KYC sessions, contract management, electronic signatures, and audit trails.

-- Enable pgcrypto extension for UUID generation if not already available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: kyc_sessions
-- Stores temporary session data for Identity Verification (KYC)
CREATE TABLE kyc_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    extracted_data JSONB,
    biometric_score DECIMAL(5, 2),
    CONSTRAINT ck_kyc_status CHECK (status IN ('CREATED', 'DOCUMENT_UPLOADED', 'BIOMETRIC_COMPLETED', 'VERIFIED', 'FAILED', 'PROCESSING', 'MANUAL_REVIEW', 'APPROVED', 'REJECTED'))
);

-- Table: contracts
-- Manages the lifecycle and metadata of digital contracts
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    document_hash_sha256 VARCHAR(64) NOT NULL,
    minio_uri VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_contract_status CHECK (status IN ('PREPARED', 'SIGNED', 'REVOKED', 'DRAFT', 'PENDING_SIGNATURE', 'CANCELLED', 'EXPIRED'))
);

-- Table: signatures
-- Records individual signature events associated with a contract
CREATE TABLE signatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    signer_info JSONB NOT NULL,
    x509_certificate_sn VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Table: audit_trails
-- Immutable legal evidence consolidating KYC and signature metadata
CREATE TABLE audit_trails (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    kyc_session_id UUID NOT NULL REFERENCES kyc_sessions(id) ON DELETE CASCADE,
    trail_manifest JSONB NOT NULL,
    final_signed_pdf_uri VARCHAR(255)
);

-- Indices for kyc_sessions performance
CREATE INDEX idx_kyc_sessions_status ON kyc_sessions(status);
CREATE INDEX idx_kyc_sessions_expires_at ON kyc_sessions(expires_at);

-- Indices for contracts performance
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_template_id ON contracts(template_id);
CREATE INDEX idx_contracts_created_at ON contracts(created_at);

-- Indices for signatures performance
CREATE INDEX idx_signatures_contract_id ON signatures(contract_id);
CREATE INDEX idx_signatures_x509_certificate_sn ON signatures(x509_certificate_sn);

-- Indices for audit_trails performance
CREATE INDEX idx_audit_trails_contract_id ON audit_trails(contract_id);
CREATE INDEX idx_audit_trails_kyc_session_id ON audit_trails(kyc_session_id);
