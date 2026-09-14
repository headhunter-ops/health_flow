CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY ,
    claim_id VARCHAR(50) UNIQUE NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    claim_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);