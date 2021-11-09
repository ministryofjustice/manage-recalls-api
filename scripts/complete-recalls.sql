-- A script to move open recalls to DOSSIER_ISSUED status.

update recall
set dossier_created_by_user_id = '00000000-0000-0000-0000-000000000000',
    dossier_email_sent_date = '2021-11-09',
    last_updated_date_time = now()
where dossier_created_by_user_id is null