# Testing Centralized ID Generation

## Overview
This document outlines how to test the centralized ID generation implementation.

## Test Steps

### 1. Start All Services
```bash
docker-compose up -d
```

### 2. Test Vendor Registration
```bash
# Register a new vendor
curl -X POST "http://localhost:8083/vendors/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testvendor",
    "email": "vendor@test.com",
    "password": "password123",
    "address": "123 Test St",
    "phone": "1234567890",
    "zipCode": "12345",
    "businessType": "restaurant"
  }'
```

**Expected Result:**
- Vendor gets registered with a global ID from authorization service
- Same ID should be used in both vendor-service and authorization-service databases

### 3. Test User Registration
```bash
# Register a new user
curl -X POST "http://localhost:8081/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "user@test.com",
    "password": "password123"
  }'
```

**Expected Result:**
- User gets registered with a global ID from authorization service
- Same ID should be used in both user-service and authorization-service databases

### 4. Verify ID Consistency

#### Check Authorization Service Database
```sql
SELECT id, email, username, role, status FROM users ORDER BY id;
```

#### Check Vendor Service Database
```sql
SELECT id, email, username FROM vendors ORDER BY id;
```

#### Check User Service Database
```sql
SELECT id, email, username FROM users ORDER BY id;
```

**Expected Result:**
- All services should have the same IDs for the same users
- No ID conflicts or mismatches

### 5. Test ID Generation API Directly

#### Generate User ID
```bash
curl -X POST "http://localhost:8082/api/auth/generate-user-id" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "direct@test.com",
    "username": "directuser",
    "role": "USER"
  }'
```

**Expected Result:**
- Returns a global user ID
- ID should be unique and consistent

### 6. Test User Verification

#### Verify Vendor
```bash
# Get verification token from vendor registration response
curl -X GET "http://localhost:8083/vendors/verify?token=VERIFICATION_TOKEN"
```

#### Verify User
```bash
# Get verification token from user registration response
curl -X GET "http://localhost:8081/users/verify?token=VERIFICATION_TOKEN"
```

**Expected Result:**
- Verification should update both local service and authorization service
- User status should be ACTIVE in both databases

## Verification Points

1. **ID Uniqueness**: No duplicate IDs across services
2. **ID Consistency**: Same user has same ID in all services
3. **Registration Flow**: Global ID is generated before local registration
4. **Verification Flow**: Verification updates both local and global records
5. **Error Handling**: Proper error handling for failed ID generation

## Troubleshooting

### Common Issues

1. **Service Not Available**: Ensure all services are running
2. **Network Issues**: Check service URLs in client configurations
3. **Database Issues**: Verify database connections
4. **ID Conflicts**: Check for existing users with same email

### Logs to Check

- Authorization Service: `docker logs authorization-service`
- Vendor Service: `docker logs vendor-service`
- User Service: `docker logs user-service`

## Expected Database State

After successful testing, you should see:

### Authorization Service (users table)
```
id | email           | username    | role   | status
---|-----------------|-------------|--------|--------
1  | vendor@test.com | testvendor  | VENDOR | INACTIVE
2  | user@test.com   | testuser    | USER   | INACTIVE
3  | direct@test.com | directuser  | USER   | INACTIVE
```

### Vendor Service (vendors table)
```
id | email           | username
---|-----------------|----------
1  | vendor@test.com | testvendor
```

### User Service (users table)
```
id | email         | username
---|---------------|----------
2  | user@test.com | testuser
```

**Key Point**: The IDs should match across services for the same users!
