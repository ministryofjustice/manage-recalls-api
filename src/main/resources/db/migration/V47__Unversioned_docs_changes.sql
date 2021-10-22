ALTER TABLE recall_document RENAME to versioned_document;

ALTER TABLE versioned_document ADD COLUMN created_date_time TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE unversioned_document
(
    id        UUID PRIMARY KEY,
    recall_id UUID NOT NULL REFERENCES recall (id),
    category  TEXT NOT NULL,
    file_name TEXT NOT NULL,
    created_date_time TIMESTAMPTZ NOT NULL
)