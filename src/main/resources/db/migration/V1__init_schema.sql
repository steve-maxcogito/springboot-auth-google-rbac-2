CREATE TABLE IF NOT EXISTS role (
                                    id   UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL UNIQUE
);
