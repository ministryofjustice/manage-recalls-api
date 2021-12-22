ALTER TABLE recall
    ADD COLUMN last_updated_by_user_id UUID references user_details(id);

update recall set last_updated_by_user_id = created_by_user_id
    where last_updated_by_user_id is null;

ALTER TABLE recall
    ALTER COLUMN last_updated_by_user_id SET NOT NULL;