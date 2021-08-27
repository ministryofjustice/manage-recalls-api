ALTER TABLE recall RENAME COLUMN agree_with_recall_length to agree_with_recall;
ALTER TABLE recall RENAME COLUMN agree_with_recall_length_detail TO agree_with_recall_detail;
ALTER TABLE recall DROP COLUMN agree_with_recall_recommendation;
