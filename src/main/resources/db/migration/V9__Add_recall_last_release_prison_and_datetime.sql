ALTER TABLE recall
    ADD COLUMN last_release_prison varchar(32),
    ADD COLUMN last_release_date_time TIMESTAMPTZ;