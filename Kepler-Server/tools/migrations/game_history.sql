CREATE TABLE IF NOT EXISTS game_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) DEFAULT NULL,
    game_creator VARCHAR(50) DEFAULT NULL,
    map_id INT DEFAULT 0,
    winning_team INT DEFAULT -1,
    winning_team_score INT DEFAULT 0,
    extra_data VARCHAR(255) DEFAULT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_game_type (game_type),
    INDEX idx_played_at (played_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_history_players (
    id INT AUTO_INCREMENT PRIMARY KEY,
    history_id INT NOT NULL,
    user_id INT NOT NULL,
    team_id INT DEFAULT 0,
    score INT DEFAULT 0,
    INDEX idx_history (history_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (history_id) REFERENCES game_history(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
