ALTER TABLE missing_documents_record
    ADD CONSTRAINT fk_created_by_user_id FOREIGN KEY (created_by_user_id) REFERENCES user_details(id);