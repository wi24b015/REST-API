CREATE TABLE IF NOT EXISTS energy_usage (
                                            hour VARCHAR(19) PRIMARY KEY,
    community_produced DOUBLE PRECISION NOT NULL DEFAULT 0,
    community_used DOUBLE PRECISION NOT NULL DEFAULT 0,
    grid_used DOUBLE PRECISION NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS current_percentage (
                                                  hour VARCHAR(19) PRIMARY KEY,
    community_depleted DOUBLE PRECISION NOT NULL DEFAULT 0,
    grid_portion DOUBLE PRECISION NOT NULL DEFAULT 0
    );