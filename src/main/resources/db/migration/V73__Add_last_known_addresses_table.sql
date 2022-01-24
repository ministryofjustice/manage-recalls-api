CREATE TABLE last_known_address (
          id UUID PRIMARY KEY,
          recall_id UUID NOT NULL REFERENCES recall (id),
          line1 TEXT NOT NULL,
          line2 TEXT NULL,
          town TEXT NOT NULL,
          postcode TEXT NULL,
          source TEXT NOT NULL,
          index INTEGER NOT NULL,
          created_by_user_id UUID references user_details(id),
          created_date_time TIMESTAMPTZ NOT NULL,
          UNIQUE (recall_id, index),
          CONSTRAINT index_positive_integer CHECK (index >= 1)
);

-- DROP TABLE last_known_address;