CREATE TABLE idempotency_records (
                                     id BIGSERIAL PRIMARY KEY,

                                     idempotency_key VARCHAR(100) NOT NULL UNIQUE,

                                     request_hash VARCHAR(255),

                                     response_body TEXT,

                                     status VARCHAR(50) NOT NULL,

                                     created_at TIMESTAMP NOT NULL
);