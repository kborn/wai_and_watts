-- V7: Ensure cascading delete from dataset_release to mbie_generation_record
-- Drop existing FK and recreate with ON DELETE CASCADE

ALTER TABLE mbie_generation_record
    DROP CONSTRAINT IF EXISTS fk_mbie_generation_release;

ALTER TABLE mbie_generation_record
    ADD CONSTRAINT fk_mbie_generation_release
    FOREIGN KEY (dataset_release_id)
        REFERENCES dataset_release(id)
        ON DELETE CASCADE;
