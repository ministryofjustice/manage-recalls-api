ALTER TABLE recall RENAME COLUMN revocation_order_doc_s3_key to revocation_order_id;
ALTER TABLE recall RENAME COLUMN contraband to contraband_detail;
ALTER TABLE recall RENAME COLUMN vulnerability_diversity to vulnerability_diversity_detail;
ALTER TABLE recall ALTER COLUMN contraband_detail TYPE TEXT;
ALTER TABLE recall ALTER COLUMN vulnerability_diversity_detail TYPE TEXT;