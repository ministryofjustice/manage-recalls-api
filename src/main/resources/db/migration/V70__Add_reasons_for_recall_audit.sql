create table recall_reason_audit (
                                audit_id serial primary key,
                                recall_id uuid not null references recall(id),
                                updated_by_user_id uuid not null references user_details(id),
                                updated_date_time TIMESTAMPTZ not null,
                                action text not null check (action in ('INSERT','DELETE')),
                                value_list json,
                                updated_value text,
                                query text
);

create or replace function recall_reason_audit_func() returns trigger as $body$
DECLARE
    _updated_by_user_id      UUID;
    _updated_date_time       TIMESTAMPTZ;
    _value_list              JSON;
BEGIN
    if tg_op = 'DELETE' then
        select into _updated_by_user_id, _updated_date_time r.last_updated_by_user_id, r.last_updated_date_time from recall r where r.id = old.recall_id;
        select into _value_list array_to_json(array_agg(reason_for_recall)) from recall_reason where recall_id = old.recall_id;
        insert into recall_reason_audit (recall_id, updated_by_user_id, updated_date_time, action, value_list, updated_value, query)
        values (old.recall_id, _updated_by_user_id, _updated_date_time, tg_op, _value_list, old.reason_for_recall, current_query());
        return old;
    elsif tg_op = 'INSERT' then
        select into _updated_by_user_id, _updated_date_time r.last_updated_by_user_id, r.last_updated_date_time from recall r where r.id = new.recall_id;
        select into _value_list array_to_json(array_agg(reason_for_recall)) from recall_reason where recall_id = new.recall_id;
        insert into recall_reason_audit (recall_id, updated_by_user_id, updated_date_time, action, value_list, updated_value, query)
        values (new.recall_id, _updated_by_user_id, _updated_date_time, tg_op, _value_list, new.reason_for_recall, current_query());
        return new;
    end if;
end;
$body$
    language plpgsql;


create trigger recall_reason_audit_trigger after insert or delete on recall_reason for each row execute procedure recall_reason_audit_func();
