ALTER TABLE recall
    ADD COLUMN in_custody_at_booking BOOLEAN default NULL,
    ADD COLUMN in_custody_at_assessment BOOLEAN default NULL;
UPDATE recall SET in_custody_at_booking = in_custody;
UPDATE recall SET in_custody_at_assessment = in_custody;
