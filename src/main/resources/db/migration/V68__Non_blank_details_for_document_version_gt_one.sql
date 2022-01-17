-- Allow blank document details only when version is null or 1
ALTER TABLE document ADD CONSTRAINT details_not_blank_constraint_for_version_gt_1
    CHECK ((version IS NULL) OR
           (version = 1) OR
           (details IS NOT NULL AND TRIM(details) <> ''));

-- Restrict valid values of version: null or positive: type is integer but there is no unsigned integer type
ALTER TABLE document ADD CONSTRAINT version_null_or_positive_integer
    CHECK ((version IS NULL) OR
           (version >= 1));
