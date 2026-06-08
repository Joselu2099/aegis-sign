-- V3: Persist signer IDs as JSONB array on contracts table
ALTER TABLE contracts
    ADD COLUMN signer_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
