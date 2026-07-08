-- Az értékelő emailekhez: bővített review mezők + meghívó tábla.

ALTER TABLE reviews
    ADD COLUMN pros            VARCHAR(1000),
    ADD COLUMN cons            VARCHAR(1000),
    ADD COLUMN delivery_rating INTEGER,
    ADD COLUMN would_recommend BOOLEAN;

CREATE TABLE review_invitations (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    token           VARCHAR(64)  NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    send_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at         TIMESTAMP WITH TIME ZONE,
    last_error      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Az ütemező ez alapján keresi a küldendő emaileket.
CREATE INDEX idx_review_invitations_due
    ON review_invitations (status, send_at);
