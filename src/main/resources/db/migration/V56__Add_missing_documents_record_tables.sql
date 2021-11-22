CREATE TABLE missing_documents_record (
          id UUID PRIMARY KEY,
          recall_id UUID NOT NULL REFERENCES recall (id),
          email_id UUID NOT NULL REFERENCES document (id),
          detail TEXT NOT NULL,
          version INTEGER NOT NULL,
          created_by_user_id UUID NOT NULL,
          created_date_time TIMESTAMPTZ NOT NULL,
          UNIQUE (recall_id, version)
);

CREATE TABLE missing_document_category (
    missing_document_record_id    UUID    NOT NULL REFERENCES missing_documents_record (id),
    document_category   TEXT NOT NULL,
    UNIQUE (missing_document_record_id, document_category)
);