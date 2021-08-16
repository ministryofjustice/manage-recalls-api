ALTER TABLE recall RENAME COLUMN recall_email_received_datetime to recall_email_received_date_time;
ALTER TABLE recall DROP COLUMN last_release_date_time;
ALTER TABLE recall ADD COLUMN last_release_date DATE;
