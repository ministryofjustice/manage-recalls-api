-- Correct migration on addition of in_custody, based on promotion date-time
UPDATE recall SET in_custody = true
  WHERE created_date_time < TIMESTAMPTZ('2022-01-19T10:00:00Z')
  AND in_custody = false;
