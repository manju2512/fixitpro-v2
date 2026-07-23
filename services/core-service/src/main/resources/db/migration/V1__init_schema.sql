-- V1__init_schema.sql
-- FixitPro v2 core schema

CREATE TABLE role (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE users (
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    role_id         BIGINT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES role(role_id)
) ENGINE=InnoDB;

CREATE INDEX idx_users_role ON users(role_id);

CREATE TABLE service_type (
    service_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(60) NOT NULL UNIQUE,
    description     TEXT,
    base_price      DECIMAL(10,2) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE technician_profile (
    technician_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE,
    service_type_id   BIGINT NOT NULL,
    bio               TEXT,
    years_experience  INT NOT NULL DEFAULT 0,
    is_available      BOOLEAN NOT NULL DEFAULT TRUE,
    rating_avg        DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count      INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tech_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tech_service_type FOREIGN KEY (service_type_id) REFERENCES service_type(service_type_id)
) ENGINE=InnoDB;

CREATE INDEX idx_tech_service_type ON technician_profile(service_type_id);
CREATE INDEX idx_tech_availability ON technician_profile(is_available);

CREATE TABLE business_schedule (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date        DATE NOT NULL UNIQUE,
    open_time   TIME NOT NULL,
    close_time  TIME NOT NULL,
    is_closed   BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE reservation (
    reservation_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id       BIGINT NOT NULL,
    technician_id     BIGINT,
    service_type_id   BIGINT NOT NULL,
    reservation_date  DATE NOT NULL,
    time_slot         VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    address           VARCHAR(255) NOT NULL,
    telephone         VARCHAR(20) NOT NULL,
    comments          TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_customer FOREIGN KEY (customer_id) REFERENCES users(user_id),
    CONSTRAINT fk_res_technician FOREIGN KEY (technician_id) REFERENCES technician_profile(technician_id),
    CONSTRAINT fk_res_service_type FOREIGN KEY (service_type_id) REFERENCES service_type(service_type_id),
    CONSTRAINT chk_res_status CHECK (status IN ('PENDING','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_res_customer ON reservation(customer_id);
CREATE INDEX idx_res_technician ON reservation(technician_id);
CREATE INDEX idx_res_date_status ON reservation(reservation_date, status);

CREATE TABLE review (
    review_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id  BIGINT NOT NULL UNIQUE,
    customer_id     BIGINT NOT NULL,
    rating          TINYINT NOT NULL,
    comment         TEXT,
    is_edited       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users(user_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE review_reply (
    reply_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id       BIGINT NOT NULL UNIQUE,
    technician_id   BIGINT NOT NULL,
    reply_text      TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    moderated_by    BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reply_review FOREIGN KEY (review_id) REFERENCES review(review_id) ON DELETE CASCADE,
    CONSTRAINT fk_reply_technician FOREIGN KEY (technician_id) REFERENCES technician_profile(technician_id),
    CONSTRAINT fk_reply_moderator FOREIGN KEY (moderated_by) REFERENCES users(user_id),
    CONSTRAINT chk_reply_status CHECK (status IN ('VISIBLE','HIDDEN','DELETED'))
) ENGINE=InnoDB;

CREATE TABLE chat_conversation (
    conversation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at        TIMESTAMP NULL,
    CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_conv_user ON chat_conversation(user_id);

CREATE TABLE chat_message (
    message_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender          VARCHAR(10) NOT NULL,
    content         TEXT NOT NULL,
    tool_call_json  JSON NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(conversation_id) ON DELETE CASCADE,
    CONSTRAINT chk_msg_sender CHECK (sender IN ('USER','AI'))
) ENGINE=InnoDB;

CREATE INDEX idx_msg_conversation ON chat_message(conversation_id);
