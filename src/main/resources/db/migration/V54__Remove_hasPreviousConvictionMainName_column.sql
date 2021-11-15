UPDATE recall set previous_conviction_main_name_category = 'OTHER' where has_other_previous_conviction_main_name;

ALTER TABLE recall DROP COLUMN has_other_previous_conviction_main_name;