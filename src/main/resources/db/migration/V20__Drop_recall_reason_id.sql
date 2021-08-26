DROP TABLE recall_reason;

CREATE TABLE recall_reason
(
    recall_id           UUID                     NOT NULL REFERENCES recall (id),
    reason_for_recall   VARCHAR(32) NOT NULL,
    UNIQUE (recall_id, reason_for_recall)
);
