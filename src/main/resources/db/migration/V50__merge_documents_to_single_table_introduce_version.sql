ALTER TABLE versioned_document ADD COLUMN version INTEGER;
ALTER TABLE versioned_document DROP CONSTRAINT recall_document_recall_id_category_key;  -- as created by previous 'UNIQUE (recall_id, category)'
-- following only constrains where 'version' is non-null because of the SQL Standard feature that null != null, e.g. as per https://www.postgresql.org/docs/current/functions-comparison.html
ALTER TABLE versioned_document ADD CONSTRAINT unique_recall_category_version_non_null UNIQUE (recall_id, category, version);

ALTER TABLE unversioned_document ADD COLUMN version INTEGER DEFAULT NULL;
INSERT INTO versioned_document SELECT * FROM unversioned_document;
DROP TABLE unversioned_document;
ALTER TABLE versioned_document RENAME to document;
