ALTER TABLE recall ADD COLUMN returned_to_custody_date_time TIMESTAMPTZ,
                   ADD COLUMN returned_to_custody_notification_date_time TIMESTAMPTZ,
                   ADD COLUMN returned_to_custody_recorded_by_user_id UUID,
                   ADD COLUMN returned_to_custody_recorded_date_time TIMESTAMPTZ;


ALTER TABLE recall ADD CONSTRAINT returned_to_custody_fields_not_null_except_user_id
    CHECK ((returned_to_custody_date_time IS NULL and returned_to_custody_notification_date_time IS NULL and returned_to_custody_recorded_by_user_id IS NULL and returned_to_custody_recorded_date_time IS NULL) OR
           (returned_to_custody_date_time IS NOT NULL and returned_to_custody_notification_date_time IS NOT NULL and returned_to_custody_recorded_date_time IS NOT NULL));