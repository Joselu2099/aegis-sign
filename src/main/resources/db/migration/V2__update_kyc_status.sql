-- V2: Alinear CHECK constraint de kyc_sessions con GOAL.md (5 estados)
ALTER TABLE kyc_sessions DROP CONSTRAINT ck_kyc_status;

UPDATE kyc_sessions SET status = 'PENDING_DOCUMENTS' WHERE status = 'CREATED';

ALTER TABLE kyc_sessions
    ADD CONSTRAINT ck_kyc_status
    CHECK (status IN ('PENDING_DOCUMENTS', 'PROCESSING', 'MANUAL_REVIEW', 'APPROVED', 'REJECTED'));
