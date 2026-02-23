-- V2: Dataset lineage tables per engineering/design/001-architecture.md

-- Note: H2 (used in tests) does not support `SET search_path`. Postgres defaults to `public`.

-- dataset_source: UUID PK, metadata about a dataset provider/source
CREATE TABLE IF NOT EXISTS dataset_source (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    publisher TEXT NOT NULL, -- LAWA | MBIE
    source_url TEXT NOT NULL UNIQUE,
    expected_format TEXT NOT NULL, -- CSV | XLSX | ZIP
    update_cadence TEXT NULL
);

-- dataset_release: UUID PK, references dataset_source
CREATE TABLE IF NOT EXISTS dataset_release (
    id UUID PRIMARY KEY,
    dataset_source_id UUID NOT NULL REFERENCES dataset_source (id) ON DELETE CASCADE,
    published_date DATE NOT NULL,
    release_label TEXT NULL,
    retrieved_at TIMESTAMP NOT NULL,
    imported_at TIMESTAMP NULL,
    content_hash TEXT NOT NULL,
    status TEXT NOT NULL, -- PENDING | IMPORTED | FAILED
    notes TEXT NULL,
    CONSTRAINT uq_dataset_release_source_hash UNIQUE (dataset_source_id, content_hash)
);
