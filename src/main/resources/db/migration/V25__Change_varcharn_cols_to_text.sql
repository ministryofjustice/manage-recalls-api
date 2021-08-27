ALTER TABLE recall
ALTER
COLUMN recall_type TYPE TEXT,
    ALTER
COLUMN recall_length TYPE TEXT,
    ALTER
COLUMN last_release_prison TYPE TEXT,
    ALTER
COLUMN local_police_force TYPE TEXT,
    ALTER
COLUMN mappa_level TYPE TEXT,
    ALTER
COLUMN sentencing_court TYPE TEXT,
    ALTER
COLUMN index_offence TYPE TEXT,
    ALTER
COLUMN booking_number TYPE TEXT,
    ALTER
COLUMN probation_officer_name TYPE TEXT,
    ALTER
COLUMN probation_officer_phone_number TYPE TEXT,
    ALTER
COLUMN probation_officer_email TYPE TEXT,
    ALTER
COLUMN probation_division TYPE TEXT,
    ALTER
COLUMN authorising_assistant_chief_officer TYPE TEXT,
    ALTER
COLUMN agree_with_recall TYPE TEXT;

ALTER TABLE recall_document
ALTER
COLUMN category TYPE TEXT;

ALTER TABLE recall_reason
ALTER
COLUMN reason_for_recall TYPE TEXT;
