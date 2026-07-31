# Admin MoR Payment Management Endpoints

This document describes the admin endpoints for managing MoR (Merchant of Record) vendor payments and payouts.

## Base URL
`/admin/mor`

## Authentication
All endpoints require admin authentication:
- **Role Required**: `ADMIN` or `SUPER_ADMIN`
- **Header**: `Authorization: Bearer <jwt_token>`

---

## Endpoints

### 1. Get All Pending Payouts

**Endpoint**: `GET /admin/mor/payouts/pending`

**Description**: Returns all payouts with status `PENDING` or `PROCESSING` for MoR vendors, including vendor information and bank details needed for processing.

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "vendorId": 1,
    "vendorName": "vendor123",
    "vendorEmail": "vendor@example.com",
    "amount": 5000,
    "currency": "EUR",
    "method": "BANK",
    "status": "PENDING",
    "requestedAt": "2024-01-14T10:00:00Z",
    "processedAt": null,
    "transactionReference": null,
    "notes": null,
    "bankDetails": {
      "bankAccountHolderName": "John Doe",
      "bankAccountNumber": "1234567890",
      "bankName": "Example Bank",
      "bankSwiftCode": "EXAMUS33",
      "bankIban": "GB82WEST12345698765432"
    }
  }
]
```

**Use Case**: View all payouts that need to be processed manually.

---

### 2. Get All MoR Vendors with Balances

**Endpoint**: `GET /admin/mor/vendors/balances`

**Description**: Returns all MoR vendors with their current balance, pending payout counts, and bank details status.

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "username": "vendor123",
    "email": "vendor@example.com",
    "country": "RS",
    "balance": 5000.00,
    "currency": "EUR",
    "hasBankDetails": true,
    "pendingPayoutsCount": 2,
    "totalPendingPayouts": 8000.00
  }
]
```

**Use Case**: Overview of all MoR vendors and their payment status.

---

### 3. Get All MoR Order Payments

**Endpoint**: `GET /admin/mor/transactions/orders?from=<date>&to=<date>`

**Description**: Returns all `ORDER_PAYMENT` transactions for MoR vendors, showing orders and amounts that need to be paid. Can be filtered by date range.

**Query Parameters**:
- `from` (optional): Start date in ISO 8601 format (e.g., `2024-01-01T00:00:00Z`)
- `to` (optional): End date in ISO 8601 format (e.g., `2024-01-31T23:59:59Z`)

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "vendorId": 1,
    "vendorName": "vendor123",
    "vendorEmail": "vendor@example.com",
    "orderId": 123,
    "amount": 2500,
    "currency": "EUR",
    "description": "Payment for order #123",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

**Use Case**: View all orders that generated payments to MoR vendors, with optional date filtering.

---

### 4. Get MoR Payout Summary

**Endpoint**: `GET /admin/mor/payouts/summary`

**Description**: Returns summary statistics for MoR payouts including counts and totals by status.

**Response** (200 OK):
```json
{
  "pendingCount": 5,
  "processingCount": 2,
  "completedCount": 10,
  "failedCount": 1,
  "pendingTotal": 25000.00,
  "processingTotal": 10000.00,
  "totalPendingAmount": 35000.00
}
```

**Use Case**: Quick overview of payout statistics and total amounts that need to be paid.

---

### 5. Update Payout Status

**Endpoint**: `PUT /admin/mor/payouts/{payoutId}/status`

**Description**: Updates the status of a payout (`PROCESSING`, `COMPLETED`, or `FAILED`). Can also add transaction reference and notes.

**Path Parameters**:
- `payoutId`: Payout ID (number)

**Query Parameters**:
- `status` (required): New status - `PROCESSING`, `COMPLETED`, or `FAILED`
- `transactionReference` (optional): Bank transfer reference or transaction ID
- `notes` (optional): Admin notes about the payout

**Request Example**:
```
PUT /admin/mor/payouts/1/status?status=COMPLETED&transactionReference=TXN123456&notes=Processed via bank transfer
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Payout status updated successfully"
}
```

**Use Case**: Mark payouts as processing, completed, or failed after manual processing.

**Status Flow**:
- `PENDING` → `PROCESSING` (when you start processing)
- `PROCESSING` → `COMPLETED` (when payment is successful)
- `PROCESSING` → `FAILED` (if payment fails)

---

### 6. Get Payouts for Specific Vendor

**Endpoint**: `GET /admin/mor/vendors/{vendorId}/payouts`

**Description**: Returns all payouts for a specific MoR vendor, including all statuses.

**Path Parameters**:
- `vendorId`: Vendor ID (number)

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "vendorId": 1,
    "amount": 5000,
    "currency": "EUR",
    "method": "BANK",
    "status": "COMPLETED",
    "requestedAt": "2024-01-14T10:00:00Z",
    "processedAt": "2024-01-15T14:30:00Z",
    "transactionReference": "TXN123456",
    "notes": "Processed via bank transfer"
  }
]
```

**Use Case**: View complete payout history for a specific vendor.

---

## Workflow Example

### Processing a Payout

1. **View Pending Payouts**:
   ```
   GET /admin/mor/payouts/pending
   ```
   - See all payouts that need processing
   - Review bank details for each vendor

2. **Mark as Processing**:
   ```
   PUT /admin/mor/payouts/1/status?status=PROCESSING
   ```
   - Indicates you've started processing the payout

3. **Process Payment**:
   - Transfer funds via bank transfer, Wise, etc.
   - Note the transaction reference

4. **Mark as Completed**:
   ```
   PUT /admin/mor/payouts/1/status?status=COMPLETED&transactionReference=TXN123456&notes=Processed via bank transfer on 2024-01-15
   ```
   - Updates status and records transaction details

---

## Data Tables

The endpoints query the following database tables:

1. **`vendor`** - Vendor information and MoR fields:
   - `payout_model` - Must be `'MOR'`
   - `balance` - Current balance (in cents)
   - `bank_account_holder_name`, `bank_account_number`, `bank_name`, `bank_swift_code`, `bank_iban`

2. **`vendor_payouts`** - Payout requests:
   - `vendor_id` - Vendor ID
   - `amount` - Payout amount (in cents)
   - `status` - `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`
   - `requested_at`, `processed_at`
   - `transaction_reference`, `notes`

3. **`vendor_balance_transactions`** - Transaction history:
   - `vendor_id` - Vendor ID
   - `order_id` - Order ID (for ORDER_PAYMENT type)
   - `amount` - Transaction amount (in cents)
   - `type` - `ORDER_PAYMENT`, `PAYOUT`, `ADJUSTMENT`, `REFUND`
   - `created_at` - Transaction date

---

## Notes

- All amounts are in **cents** (divide by 100 for display)
- Default currency is **EUR** (can be made dynamic)
- Payouts are processed **manually** by platform admins
- Bank details are included in pending payouts response for easy access
- Date filters use **ISO 8601** format: `2024-01-15T10:30:00Z`

