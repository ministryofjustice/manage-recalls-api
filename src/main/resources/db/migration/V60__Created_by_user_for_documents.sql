ALTER TABLE document
    ADD COLUMN created_by_user_id UUID references user_details(id);

update document d set created_by_user_id = r.created_by_user_id
from recall r
where d.created_by_user_id is null;

ALTER TABLE document
    ALTER COLUMN created_by_user_id SET NOT NULL;
