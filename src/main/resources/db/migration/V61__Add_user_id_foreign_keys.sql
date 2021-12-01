ALTER TABLE missing_documents_record
    ALTER COLUMN created_by_user_id SET NOT NULL;

ALTER TABLE recall
    ADD CONSTRAINT fk_assignee FOREIGN KEY (assignee) REFERENCES user_details(id),
    ADD CONSTRAINT fk_assessed_by_user_id FOREIGN KEY (assessed_by_user_id) REFERENCES user_details(id),
    ADD CONSTRAINT fk_booked_by_user_id FOREIGN KEY (booked_by_user_id) REFERENCES user_details(id),
    ADD CONSTRAINT fk_dossier_created_by_user_id FOREIGN KEY (dossier_created_by_user_id) REFERENCES user_details(id),
    ADD CONSTRAINT fk_created_by_user_id FOREIGN KEY (created_by_user_id) REFERENCES user_details(id);
