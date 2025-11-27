-- Create user_player_type table
CREATE TABLE IF NOT EXISTS user_player_type (
    user_id UUID PRIMARY KEY,
    primary_player_type VARCHAR(50) NOT NULL,
    achiever_score REAL,
    player_score REAL,
    socialiser_score REAL,
    free_spirit_score REAL,
    philanthropist_score REAL,
    disruptor_score REAL
);

-- Create index on primary_player_type for faster queries
CREATE INDEX IF NOT EXISTS idx_user_player_type_primary 
    ON user_player_type(primary_player_type);
