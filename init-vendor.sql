CREATE DATABASE stillfresh_vendordb;
CREATE USER stillfreshvendor WITH PASSWORD 'FreshStill011Vendor';
GRANT ALL PRIVILEGES ON DATABASE stillfresh_vendordb TO stillfreshvendor;
ALTER USER stillfreshvendor WITH PASSWORD 'FreshStill011Vendor';
