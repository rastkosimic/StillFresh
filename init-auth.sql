-- File: authorization-service/init.sql
CREATE DATABASE stillfresh_authdb;
CREATE USER stillfreshauth WITH PASSWORD 'FreshStill011Auth';
GRANT ALL PRIVILEGES ON DATABASE stillfresh_authdb TO stillfreshauth;
ALTER USER stillfreshauth WITH PASSWORD 'FreshStill011Auth';

-- Create initial tables if needed
-- Example: Users table for storing user data
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert initial super-admin user
INSERT INTO users (username, email, password, role, active) VALUES 
('super admin', 'super_admin@stillfresh.com', '$2a$10$l4y6sYVoBMkcsvOJi8wx/.WQZUPDj9qPjNeWuoDWOcOKxACIYayQ6', 'SUPER_ADMIN', TRUE);
