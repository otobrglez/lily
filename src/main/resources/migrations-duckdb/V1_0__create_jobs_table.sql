CREATE TABLE jobs
(
    id         INTEGER PRIMARY KEY,
    title      VARCHAR,
    text       VARCHAR,
    points     INTEGER,
    created_at TIMESTAMPTZ
);