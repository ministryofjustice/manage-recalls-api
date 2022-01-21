ALTER TABLE recall
    ADD COLUMN in_custody BOOLEAN default NULL;
UPDATE recall SET in_custody = false;