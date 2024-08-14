CREATE DATABASE stillfresh_userdb;
CREATE USER stillfreshuser WITH PASSWORD 'FreshStill011User';
GRANT ALL PRIVILEGES ON DATABASE stillfresh_userdb TO stillfreshuser;
ALTER USER stillfreshuser WITH PASSWORD 'FreshStill011User';
