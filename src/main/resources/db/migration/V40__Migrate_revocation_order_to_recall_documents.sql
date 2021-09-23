INSERT INTO recall_document SELECT revocation_order_id, ID, 'REVOCATION_ORDER', null FROM RECALL;

ALTER TABLE recall DROP COLUMN revocation_order_id;