CREATE TABLE processed_events (
                                  id BIGSERIAL PRIMARY KEY,

                                  event_id VARCHAR(100) UNIQUE NOT NULL,

                                  processed_at TIMESTAMP NOT NULL
);