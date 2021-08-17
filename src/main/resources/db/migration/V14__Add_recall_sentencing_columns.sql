ALTER TABLE recall ADD COLUMN sentence_date DATE,
    ADD COLUMN licence_expiry_date DATE,
    ADD COLUMN sentence_expiry_date DATE,
    ADD COLUMN sentencing_court VARCHAR(64),
    ADD COLUMN index_offence VARCHAR(64),
    ADD COLUMN conditional_release_date DATE;