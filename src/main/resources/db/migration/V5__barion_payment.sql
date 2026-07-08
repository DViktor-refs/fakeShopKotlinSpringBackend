-- Barion fizetés nyomon követéséhez szükséges mezők a rendeléseken.

ALTER TABLE orders
    ADD COLUMN payment_status     VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN payment_id         VARCHAR(64),
    ADD COLUMN payment_request_id VARCHAR(64),
    ADD COLUMN barion_status      VARCHAR(30);

-- Egy Barion PaymentId csak egy rendeléshez tartozhat (ha ki van töltve).
CREATE UNIQUE INDEX uq_orders_payment_id ON orders (payment_id) WHERE payment_id IS NOT NULL;
