-- FakeShop kezdeti séma
-- A products tábla a dummyjson termékstruktúráját képezi le.

CREATE TABLE products (
    id                      BIGINT          PRIMARY KEY,
    title                   VARCHAR(255)    NOT NULL,
    description             TEXT,
    category                VARCHAR(100)    NOT NULL,
    -- Szerkeszthető mezők:
    price                   NUMERIC(12, 2)  NOT NULL,
    discount_percentage     NUMERIC(5, 2)   NOT NULL,
    rating                  NUMERIC(3, 2)   NOT NULL,
    stock                   INTEGER         NOT NULL,
    -- Csak olvasható mezők:
    brand                   VARCHAR(150),
    sku                     VARCHAR(100),
    weight                  INTEGER,
    dim_width               NUMERIC(8, 2),
    dim_height              NUMERIC(8, 2),
    dim_depth               NUMERIC(8, 2),
    warranty_information    VARCHAR(255),
    shipping_information    VARCHAR(255),
    availability_status     VARCHAR(50),
    return_policy           VARCHAR(255),
    minimum_order_quantity  INTEGER,
    barcode                 VARCHAR(100),
    qr_code                 VARCHAR(512),
    meta_created_at         TIMESTAMP WITH TIME ZONE,
    meta_updated_at         TIMESTAMP WITH TIME ZONE,
    thumbnail               VARCHAR(512),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_brand    ON products (brand);
CREATE INDEX idx_products_price    ON products (price);
CREATE INDEX idx_products_rating   ON products (rating);

-- Címkék (tags) – ElementCollection
CREATE TABLE product_tags (
    product_id  BIGINT       NOT NULL,
    tag         VARCHAR(100) NOT NULL,
    CONSTRAINT pk_product_tags PRIMARY KEY (product_id, tag),
    CONSTRAINT fk_product_tags_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);

-- Képek (images) – rendezett ElementCollection
CREATE TABLE product_images (
    product_id  BIGINT       NOT NULL,
    position    INTEGER      NOT NULL,
    image_url   VARCHAR(512) NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (product_id, position),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);

-- Vélemények (reviews) – külön tábla, bővíthető
CREATE TABLE reviews (
    id              BIGSERIAL    PRIMARY KEY,
    product_id      BIGINT       NOT NULL,
    rating          INTEGER      NOT NULL,
    comment         VARCHAR(1000),
    review_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewer_name   VARCHAR(150),
    reviewer_email  VARCHAR(200),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
