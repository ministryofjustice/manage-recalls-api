CREATE TABLE last_known_address (
          id UUID PRIMARY KEY,
          recall_id UUID NOT NULL REFERENCES recall (id),
          line1 TEXT NOT NULL,
          line2 TEXT NULL,
          town TEXT NOT NULL,
          postcode TEXT NULL,
          source TEXT NOT NULL,
          index INTEGER NOT NULL,
          created_by_user_id UUID NOT NULL references user_details(id),
          created_date_time TIMESTAMPTZ NOT NULL,
          UNIQUE (recall_id, index),
          CONSTRAINT source_in_enum CHECK (source IN ('MANUAL','LOOKUP')),
          CONSTRAINT index_positive_integer CHECK (index >= 1)
);
