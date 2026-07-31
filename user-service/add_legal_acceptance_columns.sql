-- Legal acceptance audit for customers.
-- terms_version / privacy_version are supplied by the client (document version displayed);
-- *_accepted_at timestamps are stamped server-side at acceptance.
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_accepted_at   TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_version       VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS privacy_accepted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS privacy_version     VARCHAR(64);
