CREATE TABLE rescind_record (
                                id UUID PRIMARY KEY,
                                recall_id UUID NOT NULL REFERENCES recall (id),
                                version INTEGER NOT NULL,
                                created_by_user_id UUID NOT NULL,
                                created_date_time TIMESTAMPTZ NOT NULL,
                                last_updated_date_time TIMESTAMPTZ NOT NULL,
                                request_email_id UUID NOT NULL REFERENCES document (id),
                                request_email_received_date DATE NOT NULL,
                                request_details TEXT NOT NULL,
                                approved boolean,
                                decision_email_id UUID REFERENCES document (id),
                                decision_email_sent_date DATE,
                                decision_details TEXT,

                                UNIQUE (recall_id, version)
);

ALTER TABLE rescind_record ADD CONSTRAINT all_decision_fields_not_null_once_decided
    CHECK ((approved IS NULL and decision_email_id IS NULL and decision_email_sent_date IS NULL and decision_details IS NULL) OR
           (approved IS NOT NULL and decision_email_id IS NOT NULL and decision_email_sent_date IS NOT NULL and decision_details IS NOT NULL));

ALTER TABLE document DROP CONSTRAINT version_constraint_by_category;

-- Allow only valid values for version versus category
ALTER TABLE document ADD CONSTRAINT version_constraint_by_category
    CHECK ((version IS NULL AND category IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL')) OR
           (version IS NOT NULL AND category NOT IN ('OTHER', 'UNCATEGORISED', 'RESCIND_REQUEST_EMAIL', 'RESCIND_DECISION_EMAIL')));
