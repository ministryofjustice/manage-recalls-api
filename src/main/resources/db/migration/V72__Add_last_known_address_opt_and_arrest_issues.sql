ALTER TABLE recall
    ADD COLUMN last_known_address_option TEXT,
    ADD COLUMN arrest_issues BOOLEAN,
    ADD COLUMN arrest_issues_detail TEXT;