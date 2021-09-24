INSERT INTO recall_document SELECT revocation_order_id, ID, 'REVOCATION_ORDER', null FROM RECALL where revocation_order_id is not null;
