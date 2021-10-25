UPDATE versioned_document set file_name = concat(category, '.pdf') where file_name is null;

ALTER TABLE versioned_document ALTER COLUMN file_name SET NOT NULL;