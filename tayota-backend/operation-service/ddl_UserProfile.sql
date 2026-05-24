

CREATE TABLE user_profile
(
    user_id    UUID        NOT NULL,
    fullname   VARCHAR(40) NOT NULL,
    phone      VARCHAR(10),
    gender     BOOLEAN,
    birth_date date,
    address    VARCHAR(255),
    avatar_url VARCHAR(255),
    CONSTRAINT pk_user_profile PRIMARY KEY (user_id)
);

ALTER TABLE user_profile
    ADD CONSTRAINT FK_USER_PROFILE_ON_USER FOREIGN KEY (user_id) REFERENCES "user" (id);