CREATE DATABASE IF NOT EXISTS ai_agent
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE ai_agent;

DROP TABLE IF EXISTS chat_history;
DROP TABLE IF EXISTS chat;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE chat (
    chat_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id)
);

CREATE TABLE chat_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    chat_id INT NOT NULL,
    user_id INT NOT NULL,
    content LONGTEXT NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chat(chat_id),
    FOREIGN KEY (user_id) REFERENCES `user`(user_id)
);