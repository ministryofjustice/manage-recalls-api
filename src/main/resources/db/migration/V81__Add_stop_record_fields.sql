ALTER TABLE recall ADD COLUMN stopped_reason TEXT,
    ADD COLUMN stopped_by_user_id UUID,
    ADD COLUMN stopped_date_time TIMESTAMPTZ;


ALTER TABLE recall ADD CONSTRAINT all_stopped_fields_not_null_once_stopped
    CHECK ((stopped_reason IS NULL and stopped_by_user_id IS NULL and stopped_date_time IS NULL) OR
           (stopped_reason IS NOT NULL and stopped_by_user_id IS NOT NULL and stopped_date_time IS NOT NULL));