CREATE TABLE phase_record (
           id UUID PRIMARY KEY,
           recall_id UUID NOT NULL REFERENCES recall (id),
           phase TEXT NOT NULL,
           started_by_user_id UUID NOT NULL REFERENCES user_details(id),
           started_date_time TIMESTAMPTZ NOT NULL,
           ended_by_user_id UUID REFERENCES user_details(id),
           ended_date_time TIMESTAMPTZ,

           UNIQUE (recall_id, phase)
);