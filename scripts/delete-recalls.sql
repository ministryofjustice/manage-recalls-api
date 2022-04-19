-- Script used to clear out old recalls (and associated data).
-- Only to be used in dev/preprod

delete from missing_document_category where missing_document_record_id in (
    select id from missing_documents_record where email_id in (
        select id from document where recall_id in (
            select id from recall where created_date_time < DATE('2022-04-04'))));

delete from missing_documents_record where email_id in (
    select id from document where recall_id in (
        select id from recall where created_date_time < DATE('2022-04-04')));

delete from phase_record where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from rescind_record where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from part_b_record where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04')
);

delete from note where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from document where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from recall_reason where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from last_known_address where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04'));

delete from recall_reason_audit where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04')
);

delete from recall_audit where recall_id in (
    select id from recall where created_date_time < DATE('2022-04-04')
);

delete from recall where created_date_time < DATE('2022-04-04');