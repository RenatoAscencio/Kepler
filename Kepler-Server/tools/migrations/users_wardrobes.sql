CREATE TABLE IF NOT EXISTS `users_wardrobes` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `slot_number` INT NOT NULL,
    `figure` VARCHAR(255) NOT NULL,
    `sex` VARCHAR(1) NOT NULL DEFAULT 'M',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `idx_user_slot` (`user_id`, `slot_number`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
