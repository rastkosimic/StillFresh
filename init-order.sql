CREATE DATABASE stillfresh_orderdb;
CREATE USER stillfreshorders WITH PASSWORD 'FreshStill011Orders';
GRANT ALL PRIVILEGES ON DATABASE stillfresh_orderdb TO stillfreshorders;
ALTER USER stillfreshorders WITH PASSWORD 'FreshStill011Orders';
