create extension IF NOT EXISTS hstore;

create table recall_audit (
                                audit_id serial primary key,
                                recall_id uuid not null references recall(id),
                                updated_by_user_id uuid not null references user_details(id),
                                updated_date_time TIMESTAMPTZ not null,
                                action text not null check (action in ('INSERT','DELETE','UPDATE')),
                                old_values json,
                                new_values json,
                                updated_values json,
                                query text
);

create or replace function recall_modified_func() returns trigger as $body$
begin
    if tg_op = 'UPDATE' then
        insert into recall_audit (recall_id, updated_by_user_id, updated_date_time, action, old_values, new_values, updated_values, query)
        values (new.id, new.last_updated_by_user_id, new.last_updated_date_time, tg_op, row_to_json(old.*), row_to_json(new.*),
                hstore_to_json(hstore(new.*) - hstore(old.*)),  current_query());
        return new;
    elsif tg_op = 'INSERT' then
        insert into recall_audit (recall_id, updated_by_user_id, updated_date_time, action, new_values, updated_values, query)
        values (new.id, new.last_updated_by_user_id, new.last_updated_date_time, tg_op, row_to_json(new.*), hstore_to_json(hstore(new.*) - hstore(old.*)), current_query());
        return new;
    end if;
end;
$body$
    language plpgsql;


create trigger recall_audit_trigger after insert or update on recall for each row execute procedure recall_modified_func();
