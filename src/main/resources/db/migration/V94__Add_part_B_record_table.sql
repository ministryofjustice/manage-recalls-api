CREATE TABLE part_b_record (

                                id UUID PRIMARY KEY,
                                recall_id UUID NOT NULL REFERENCES recall (id),
                                details TEXT NOT NULL,
                                part_b_received_date DATE NOT NULL,
                                part_b_document_id UUID NOT NULL REFERENCES document (id),
                                email_id UUID NOT NULL REFERENCES document (id),
                                oasys_document_id UUID REFERENCES document (id),
                                version INTEGER NOT NULL,
                                created_by_user_id UUID NOT NULL REFERENCES user_details(id),
                                created_date_time TIMESTAMPTZ NOT NULL,

                                UNIQUE (recall_id, version),
                                CONSTRAINT version_positive_integer CHECK (version >= 1)
);

ALTER TABLE document DROP CONSTRAINT version_constraint_by_category;

-- Allow only valid values for version versus category
ALTER TABLE document ADD CONSTRAINT version_constraint_by_category
    CHECK ((version IS NULL AND category IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL', 'NOTE_DOCUMENT', 'PART_B_EMAIL_FROM_PROBATION')) OR
           (version IS NOT NULL AND category NOT IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL', 'NOTE_DOCUMENT', 'PART_B_EMAIL_FROM_PROBATION')));
