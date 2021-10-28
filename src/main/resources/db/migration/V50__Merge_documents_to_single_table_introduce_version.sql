CREATE TABLE document
(
    id        UUID PRIMARY KEY,
    recall_id UUID NOT NULL REFERENCES recall (id),
    category  TEXT NOT NULL,
    file_name TEXT NOT NULL,
    version   INTEGER,
    created_date_time TIMESTAMPTZ NOT NULL
);
-- following only constrains where 'version' is non-null because of the SQL Standard feature that null != null, e.g. as per https://www.postgresql.org/docs/current/functions-comparison.html
ALTER TABLE document ADD CONSTRAINT unique_recall_category_version_non_null UNIQUE (recall_id, category, version);

ALTER TABLE versioned_document ADD COLUMN version INTEGER DEFAULT 1;
INSERT INTO document(id, recall_id, category, file_name, version, created_date_time)
SELECT id, recall_id, category, file_name, version, created_date_time FROM versioned_document;
DROP TABLE versioned_document;

ALTER TABLE unversioned_document ADD COLUMN version INTEGER DEFAULT NULL;
INSERT INTO document(id, recall_id, category, file_name, version, created_date_time)
SELECT id, recall_id, category, file_name, version, created_date_time FROM unversioned_document;
DROP TABLE unversioned_document;
