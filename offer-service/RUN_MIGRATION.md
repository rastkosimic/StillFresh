# Running Currency Column Migration

The `currency` column needs to be added to the `offers` table. You have two options:

## Option 1: Let Hibernate Auto-Create (Quick Fix)

Since `ddl-auto: update` is enabled, Hibernate will automatically add the column when you restart the offer-service. The column is currently set to nullable, so it will be created successfully.

**Steps:**
1. Restart the offer-service
2. Hibernate will automatically add the `currency` column
3. Existing offers will have `currency = null`
4. New offers will have currency determined automatically

**To set default values for existing offers**, run the migration script after Hibernate creates the column.

## Option 2: Run Migration Script Manually (Recommended)

**If using Docker Compose:**

```bash
# Connect to the PostgreSQL container
docker exec -it offer-postgres psql -U stillfreshoffers -d stillfresh_offerdb

# Then run the SQL commands from add_currency_column.sql
```

**Or copy the SQL file into the container and execute:**

```bash
docker cp offer-service/add_currency_column.sql offer-postgres:/tmp/
docker exec -it offer-postgres psql -U stillfreshoffers -d stillfresh_offerdb -f /tmp/add_currency_column.sql
```

**If using local PostgreSQL:**

```bash
psql -U stillfreshoffers -d stillfresh_offerdb -f offer-service/add_currency_column.sql
```

## Migration Script Contents

The migration script (`add_currency_column.sql`) will:
1. Add the `currency` column (nullable first)
2. Set default value 'EUR' for existing rows
3. Update Serbia region offers to 'RSD' (best-effort)
4. Make the column NOT NULL with default 'EUR'

## After Migration

Once the column is added, you can optionally:
1. Update the entity to make it `nullable = false` again
2. Run currency detection for existing offers to set proper currencies

