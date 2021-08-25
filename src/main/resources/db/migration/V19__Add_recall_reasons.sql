CREATE TABLE recall_reason
(
    id                  UUID PRIMARY KEY,
    recall_id           UUID                     NOT NULL REFERENCES recall (id),
    reason_for_recall   VARCHAR(32) NOT NULL,
    UNIQUE (recall_id, reason_for_recall)
);

ALTER TABLE recall ADD COLUMN licence_conditions_breached TEXT;
