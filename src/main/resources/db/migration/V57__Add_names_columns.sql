ALTER TABLE recall
    ADD COLUMN first_name TEXT,
    ADD COLUMN middle_names TEXT,
    ADD COLUMN last_name TEXT,
    ADD COLUMN licence_name_category TEXT NOT NULL DEFAULT 'FIRST_LAST';
