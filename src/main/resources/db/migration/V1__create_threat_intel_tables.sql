CREATE TABLE threat_indicators (
    id UUID PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    value VARCHAR(512) NOT NULL,
    source VARCHAR(120) NOT NULL,
    confidence SMALLINT NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    status VARCHAR(24) NOT NULL,
    technique_id VARCHAR(12),
    technique_name VARCHAR(160),
    first_seen TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    UNIQUE (type, value)
);

CREATE INDEX idx_threat_indicators_seen ON threat_indicators (first_seen DESC);
CREATE INDEX idx_threat_indicators_confidence ON threat_indicators (confidence DESC);

CREATE TABLE indicator_tags (
    indicator_id UUID NOT NULL REFERENCES threat_indicators(id) ON DELETE CASCADE,
    tag VARCHAR(40) NOT NULL,
    position SMALLINT NOT NULL,
    PRIMARY KEY (indicator_id, tag),
    UNIQUE (indicator_id, position)
);

CREATE TABLE threat_matches (
    id UUID PRIMARY KEY,
    indicator_id UUID NOT NULL REFERENCES threat_indicators(id),
    observable VARCHAR(512) NOT NULL,
    asset VARCHAR(160) NOT NULL,
    context VARCHAR(600) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_threat_matches_observed ON threat_matches (observed_at DESC);
CREATE INDEX idx_threat_matches_indicator ON threat_matches (indicator_id, observed_at DESC);