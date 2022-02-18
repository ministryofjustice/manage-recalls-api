CREATE TABLE note (
                                id UUID PRIMARY KEY,
                                recall_id UUID NOT NULL REFERENCES recall (id),
                                subject TEXT NOT NULL,
                                details TEXT NOT NULL,
                                index INTEGER NOT NULL,
                                document_id UUID REFERENCES document (id),
                                created_by_user_id UUID NOT NULL references user_details(id),
                                created_date_time TIMESTAMPTZ NOT NULL,
                                UNIQUE (recall_id, index),
                                CONSTRAINT index_positive_integer CHECK (index >= 1)
);

ALTER TABLE document DROP CONSTRAINT version_constraint_by_category;

-- Allow only valid values for version versus category
ALTER TABLE document ADD CONSTRAINT version_constraint_by_category
    CHECK ((version IS NULL AND category IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL', 'NOTE_DOCUMENT')) OR
           (version IS NOT NULL AND category NOT IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL', 'NOTE_DOCUMENT')));
