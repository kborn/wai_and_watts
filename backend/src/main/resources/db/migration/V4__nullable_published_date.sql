-- V4: Allow NULL for dataset_release.published_date per design update

ALTER TABLE dataset_release
    ALTER COLUMN published_date DROP NOT NULL;
