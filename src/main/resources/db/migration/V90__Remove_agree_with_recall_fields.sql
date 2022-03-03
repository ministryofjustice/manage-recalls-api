UPDATE recall SET recommended_recall_type = 'FIXED', confirmed_recall_type = 'FIXED' WHERE agree_with_recall = 'YES' and recommended_recall_type is not NULL;
UPDATE recall SET confirmed_recall_type_detail = agree_with_recall_detail where agree_with_recall is not null;

DELETE FROM recall WHERE agree_with_recall = 'NO_STOP';

ALTER TABLE recall DROP COLUMN agree_with_recall;
ALTER TABLE recall DROP COLUMN agree_with_recall_detail;

-- Remove audit entries for old fields from audit.
update recall_audit set updated_values = cast(updated_values as jsonb) - 'agree_with_recall'  where jsonb_exists(cast(updated_values as jsonb), 'agree_with_recall');
update recall_audit set updated_values = cast(updated_values as jsonb) - 'agree_with_recall_detail'  where jsonb_exists(cast(updated_values as jsonb), 'agree_with_recall_detail');



