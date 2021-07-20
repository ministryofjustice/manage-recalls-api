CREATE TYPE recall_document_category AS ENUM (
    'PART_A_RECALL_REPORT',
    'LICENCE',
    'OASYS_RISK_ASSESSMENT',
    'PREVIOUS_CONVICTIONS_SHEET',
    'PRE_SENTENCING_REPORT'
    );

CREATE TABLE recall_document
(
    id        UUID PRIMARY KEY,
    recall_id UUID                     NOT NULL REFERENCES recall (id),
    category  recall_document_category NOT NULL,
    UNIQUE (recall_id, category)
)

