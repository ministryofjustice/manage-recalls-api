ALTER TABLE recall ADD COLUMN created_date_time TIMESTAMPTZ NOT NULL default now(),
    ADD COLUMN last_updated_date_time TIMESTAMPTZ NOT NULL default now();

ALTER TABLE user_details ADD COLUMN created_date_time TIMESTAMPTZ NOT NULL default now();
