-- Media Ratings Platform schema (PostgreSQL)
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    password_salt   VARCHAR(255) NOT NULL,
    email           VARCHAR(200),
    favorite_genre  VARCHAR(80),
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS media (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(2000) NOT NULL DEFAULT '',
    media_type       VARCHAR(16)  NOT NULL,
    release_year     INTEGER      NOT NULL,
    age_restriction  INTEGER      NOT NULL,
    creator_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_media_type CHECK (media_type IN ('MOVIE', 'SERIES', 'GAME')),
    CONSTRAINT ck_media_year CHECK (release_year BETWEEN 1888 AND 2200),
    CONSTRAINT ck_media_age CHECK (age_restriction BETWEEN 0 AND 21)
);

CREATE TABLE IF NOT EXISTS media_genres (
    media_id  BIGINT      NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    genre     VARCHAR(80) NOT NULL,
    PRIMARY KEY (media_id, genre)
);

CREATE TABLE IF NOT EXISTS ratings (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id           BIGINT      NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    stars              INTEGER     NOT NULL,
    comment            VARCHAR(1000),
    comment_confirmed  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_rating_stars CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT uq_rating_user_media UNIQUE (user_id, media_id)
);

CREATE TABLE IF NOT EXISTS rating_likes (
    rating_id  BIGINT      NOT NULL REFERENCES ratings(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (rating_id, user_id)
);

CREATE TABLE IF NOT EXISTS favorites (
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id   BIGINT      NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, media_id)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
    token      VARCHAR(128) PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_media_creator ON media(creator_id);
CREATE INDEX IF NOT EXISTS idx_media_title ON media(LOWER(title));
CREATE INDEX IF NOT EXISTS idx_media_genres_genre ON media_genres(genre);
CREATE INDEX IF NOT EXISTS idx_ratings_media ON ratings(media_id);
CREATE INDEX IF NOT EXISTS idx_ratings_user ON ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_media ON favorites(media_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_expires ON auth_tokens(expires_at);
