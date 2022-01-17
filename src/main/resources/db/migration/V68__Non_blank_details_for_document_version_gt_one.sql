-- Patch existing data prior to constraint: Restrict valid values of version: not _expected_ to apply to any data
UPDATE document SET version = 9999
WHERE version IS NOT NULL AND
        version < 1;

-- Restrict valid values of version: null or positive: type is integer but there is no unsigned integer type
ALTER TABLE document ADD CONSTRAINT version_null_or_positive_integer
    CHECK ((version IS NULL) OR
           (version >= 1));

-- Patch existing data prior to constraint: allow blank document details only when version is null or 1
UPDATE document SET details = 'patched to non-null'
WHERE version IS NOT NULL AND
        version <> 1 AND
    (details IS NULL OR TRIM(details) = '');

-- Allow blank document details only when version is null or 1
ALTER TABLE document ADD CONSTRAINT details_not_blank_constraint_for_version_gt_1
    CHECK ((version IS NULL) OR
           (version = 1) OR
           (details IS NOT NULL AND TRIM(details) <> ''));
