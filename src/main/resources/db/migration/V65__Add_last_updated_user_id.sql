ALTER TABLE recall
    ADD COLUMN last_updated_by_user_id UUID REFERENCES user_details(id);

UPDATE recall SET last_updated_by_user_id = created_by_user_id
    WHERE last_updated_by_user_id IS NULL;

ALTER TABLE recall
    ALTER COLUMN last_updated_by_user_id SET NOT NULL;