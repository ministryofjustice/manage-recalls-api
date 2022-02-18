ALTER TABLE recall RENAME COLUMN stopped_reason TO stop_reason;
ALTER TABLE recall RENAME COLUMN stopped_by_user_id TO stop_by_user_id;
ALTER TABLE recall RENAME COLUMN stopped_date_time TO stop_date_time;


ALTER TABLE recall DROP CONSTRAINT all_stopped_fields_not_null_once_stopped;

ALTER TABLE recall ADD CONSTRAINT all_stopped_fields_not_null_once_stopped
    CHECK ((stop_reason IS NULL and stop_by_user_id IS NULL and stop_date_time IS NULL) OR
           (stop_reason IS NOT NULL and stop_by_user_id IS NOT NULL and stop_date_time IS NOT NULL));


ALTER TABLE recall
    ADD CONSTRAINT fk_stop_by_user_id FOREIGN KEY (stop_by_user_id) REFERENCES user_details(id),
    ADD CONSTRAINT fk_returned_to_custody_recorded_by_user_id FOREIGN KEY (returned_to_custody_recorded_by_user_id) REFERENCES user_details(id);

ALTER TABLE rescind_record
    ADD CONSTRAINT fk_created_by_user_id FOREIGN KEY (created_by_user_id) REFERENCES user_details(id);