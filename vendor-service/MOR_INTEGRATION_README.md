# Merchant of Record (MoR) Integration

This document describes the hybrid payment solution that integrates Merchant of Record (MoR) model alongside Stripe Connect for vendor payouts.

## Overview

The system now supports two payment models:
- **Stripe Connect**: Used for vendors in supported countries (US, EU, etc.)
- **Merchant of Record (MoR)**: Used for vendors in unsupported countries (Balkan region, etc.)

## Architecture

### Payment Model Selection

The system automatically determines the payment model based on the vendor's country:

1. **PaymentProviderService**: Determines the appropriate provider and payout model based on country code
2. **PaymentRoutingService**: Routes vendor operations to the correct processor
3. **VendorPayoutProcessor**: Interface abstraction for payment providers
   - `StripePayoutProcessor`: Handles Stripe Connect operations
   - `MoRPayoutProcessor`: Handles MoR balance tracking and manual payouts

### Database Changes

New fields added to `vendor` table:
- `payout_model`: Enum (CONNECT or MOR)
- `balance`: Internal balance for MoR vendors (in cents)
- `manual_payout_method`: Enum (BANK, WISE, OTHER)
- `bank_account_holder_name`: Bank account holder name
- `bank_account_number`: Bank account number
- `bank_name`: Bank name
- `bank_swift_code`: SWIFT/BIC code
- `bank_iban`: IBAN (if applicable)

New tables:
- `vendor_balance_transactions`: Transaction history for MoR vendor balances
- `vendor_payouts`: Manual payout requests for MoR vendors

Run the migration: `add_mor_fields.sql`

## How It Works

### Stripe Connect Model (Supported Countries)

1. Vendor registers from a supported country (e.g., Germany, US)
2. System creates Stripe Connect account automatically
3. Vendor completes Stripe onboarding
4. Payments are split: vendor amount goes directly to vendor's Stripe account, platform fee stays in platform account
5. Stripe handles payouts automatically

### MoR Model (Unsupported Countries)

1. Vendor registers from an unsupported country (e.g., Serbia, Bosnia)
2. System sets up MoR model automatically (no Stripe account created)
3. Vendor provides bank account details via `/vendors/mor/bank-details` endpoint
4. When customer pays:
   - Payment goes to platform's Stripe account (full amount)
   - Platform fee is deducted
   - Vendor's share is added to their internal `balance`
5. Vendor can request payouts via `/vendors/mor/request-payout`
6. Platform processes payouts manually (bank transfer, Wise, etc.)

## API Endpoints

### Unified Endpoints (Work with both models)

- `GET /vendors/payment/status`: Get payment account status
- `POST /vendors/payment/onboarding-link`: Get onboarding link

### MoR-Specific Endpoints

- `GET /vendors/mor/balance`: Get current balance
- `GET /vendors/mor/transactions`: Get transaction history
- `POST /vendors/mor/bank-details`: Submit bank account details
- `GET /vendors/mor/payouts`: Get payout history
- `POST /vendors/mor/request-payout`: Request manual payout

### Stripe-Specific Endpoints (Backward compatible)

All existing Stripe endpoints continue to work for Connect vendors:
- `GET /vendors/stripe/onboarding-link`
- `GET /vendors/stripe/account-status`
- `GET /vendors/stripe/account`
- `GET /vendors/stripe/balance`
- `GET /vendors/stripe/payouts`
- etc.

## Payment Processing

### Implementation

- **Payment Collection**: Always via Stripe (customer payments)
- **Vendor Payouts**:
  - Stripe-supported countries: Via Stripe Connect (automatic transfer)
  - MoR countries: Via internal balance tracking (manual payouts)

### Payment Service Integration

The `payment-service` has been fully integrated with MoR model:

1. **Payout Model Detection**: Before processing payment, `payment-service` checks vendor's payout model via `VendorClient.getVendorPayoutModel()`
2. **Payment Routing**:
   - **CONNECT vendors**: Uses Stripe Connect to split payment (vendor amount goes directly to vendor's Stripe account)
   - **MoR vendors**: Payment goes to platform account (no Stripe Connect transfer)
3. **Balance Update**: After successful payment for MoR vendors, `payment-service` calls `VendorClient.updateMoRBalance()` to add vendor's share to their internal balance
4. **Transaction Recording**: Balance update creates a transaction record in `vendor_balance_transactions` table

### Payment Flow Details

**For MoR Vendors:**
```
1. Customer pays → Payment goes to platform Stripe account (full amount)
2. Platform fee deducted → Remaining amount = vendorAmount
3. Vendor balance updated → vendorAmount added to vendor.balance
4. Transaction recorded → Entry in vendor_balance_transactions
```

**For CONNECT Vendors:**
```
1. Customer pays → Payment split via Stripe Connect
2. Vendor amount → Goes directly to vendor's Stripe account
3. Platform fee → Stays in platform account
4. No balance update needed → Stripe handles payouts automatically
```

## Country Support

### Stripe Supported Countries (Use Connect)
- North America: US, CA, MX
- Europe: GB, IE, FR, DE, IT, ES, NL, BE, AT, CH, SE, NO, DK, FI, PL, PT, GR, CZ, HU, RO, BG, HR, SI, SK, LT, LV, EE, LU, MT, CY
- Asia Pacific: AU, NZ, SG, JP, HK, MY, TH, PH, ID, VN, IN
- Latin America: BR, AR, CL, CO, PE, UY

### MoR Required Countries (Use MoR Model)
- Balkan region: RS (Serbia), BA (Bosnia), AL (Albania), MK (North Macedonia), ME (Montenegro), XK (Kosovo)
- Other unsupported countries default to MoR

## Testing

1. **Stripe Connect Flow** (e.g., Germany):
   - Register vendor with German address
   - System creates Stripe account automatically
   - Vendor completes Stripe onboarding
   - Customer makes payment
   - Payment splits automatically via Stripe Connect
   - Vendor receives payment directly in their Stripe account

2. **MoR Flow** (e.g., Serbia):
   - Register vendor with Serbian address
   - System sets up MoR model automatically
   - Vendor submits bank details via `POST /vendors/mor/bank-details`
   - Customer makes payment
   - Payment goes to platform account (full amount)
   - Vendor's balance automatically updated via `payment-service`
   - Vendor can check balance via `GET /vendors/mor/balance`
   - Vendor requests payout via `POST /vendors/mor/request-payout`
   - Platform processes payout manually (bank transfer)
   - Payout status tracked in `vendor_payouts` table

## Migration Guide

1. **Run database migration**: `add_mor_fields.sql`
   ```sql
   -- This will:
   -- - Remove payoneer_account_id column
   -- - Add MoR-specific fields (balance, bank details, payout_model)
   -- - Create vendor_balance_transactions table
   -- - Create vendor_payouts table
   ```

2. **Restart services**:
   - Restart `vendor-service`
   - Restart `payment-service` (to pick up new VendorClient methods)

3. **Existing vendors**:
   - Will be assigned payout models on next login/update
   - Vendors with `payment_provider = 'PAYONEER'` will be updated to `'MOR'`
   - Existing Stripe Connect vendors remain unchanged

4. **New vendors**:
   - Will be assigned payout models automatically on verification
   - Based on country code from address or explicit country field

## Architecture Diagram

```
┌─────────────────┐
│  Customer       │
│  (Pays via      │
│   Stripe)       │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│     payment-service                 │
│  ┌───────────────────────────────┐  │
│  │ 1. Check vendor payout model  │  │
│  │    (CONNECT or MOR)           │  │
│  └───────────┬───────────────────┘  │
│              │                       │
│    ┌─────────┴─────────┐             │
│    │                   │             │
│ CONNECT              MOR             │
│    │                   │             │
│    ▼                   ▼             │
│  ┌──────────┐      ┌──────────┐     │
│  │ Stripe   │      │ Platform │     │
│  │ Connect  │      │ Account  │     │
│  │ Transfer │      │ (Full    │     │
│  │          │      │  Amount) │     │
│  └──────────┘      └────┬─────┘     │
│                         │            │
│                         ▼            │
│              ┌──────────────────┐    │
│              │ Update MoR      │    │
│              │ Vendor Balance  │    │
│              │ (via vendor-     │    │
│              │  service API)   │    │
│              └──────────────────┘    │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│     vendor-service                  │
│  ┌───────────────────────────────┐  │
│  │ MoRPayoutProcessor            │  │
│  │ - addToBalance()               │  │
│  │ - Updates vendor.balance      │  │
│  │ - Records transaction         │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

## Troubleshooting

### MoR Balance Not Updating

1. Check `payment-service` logs for balance update calls
2. Verify vendor's payout model: `GET /vendors/{vendorId}/payout-model`
3. Check vendor-service logs for balance update errors
4. Verify vendor has `payout_model = 'MOR'` in database

### Payment Goes to Platform for CONNECT Vendor

1. Check if vendor's Stripe account is ready: `GET /vendors/stripe/account-status`
2. Verify `payout_model = 'CONNECT'` in database
3. Check if Stripe account ID exists: `GET /vendors/{vendorId}/stripe-account-id`

### Balance Update Fails But Payment Succeeds

- This is by design to prevent payment failures
- Check logs for error details
- Manually update balance if needed via admin interface (to be implemented)

## Important Notes

- **Payment Service Integration**: ✅ **COMPLETED** - The `payment-service` automatically detects MoR vendors and updates their balance after successful payments.
- **Manual Payout Processing**: MoR payouts require manual processing by platform administrators. Consider implementing an admin interface for processing payouts.
- **Bank Details Security**: Bank account details are stored in the database. Consider encryption for sensitive fields in production.
- **Balance Tracking**: All balance transactions are recorded in `vendor_balance_transactions` table for audit purposes.
- **Error Handling**: If MoR balance update fails, payment still succeeds (logged for manual fix). This prevents payment failures due to balance update issues.

## Internal API Endpoints (for payment-service)

These endpoints are used internally by `payment-service`:

- `GET /vendors/{vendorId}/payout-model`: Returns vendor's payout model (CONNECT or MOR)
- `POST /vendors/{vendorId}/mor/update-balance`: Updates MoR vendor balance after payment

These endpoints do not require authentication as they are called from trusted internal services.

## Code Structure

### Key Components

**vendor-service:**
- `PaymentProviderService`: Determines payout model based on country
- `PaymentRoutingService`: Routes operations to appropriate processor
- `MoRPayoutProcessor`: Handles MoR balance operations
- `VendorService.updateMoRBalance()`: Updates balance (called by payment-service)
- `VendorController`: Exposes MoR endpoints and internal API endpoints

**payment-service:**
- `VendorClient`: Feign client for vendor-service communication
  - `getVendorPayoutModel()`: Checks vendor's payout model
  - `updateMoRBalance()`: Updates MoR vendor balance
- `PaymentService.processPaymentRequest()`: Updated to handle MoR vendors

### Data Flow

1. **Vendor Registration**:
   - Vendor registers with country information
   - `PaymentProviderService.determinePayoutModel()` determines model
   - `initializeVendorPaymentAccount()` sets up appropriate model

2. **Payment Processing**:
   - `PaymentService` receives payment request
   - Calls `VendorClient.getVendorPayoutModel()` to check model
   - Routes payment accordingly:
     - CONNECT → Stripe Connect transfer
     - MOR → Platform account + balance update

3. **Balance Update** (MoR only):
   - After successful payment, `PaymentService` calls `VendorClient.updateMoRBalance()`
   - `VendorService.updateMoRBalance()` processes the update
   - `MoRPayoutProcessor.addToBalance()` updates balance and records transaction

## Example API Usage

### MoR Vendor Submitting Bank Details

```bash
POST /vendors/mor/bank-details
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "holderName": "John Doe",
  "accountNumber": "1234567890",
  "bankName": "National Bank of Serbia",
  "swiftCode": "NBSRRSBG",
  "iban": "RS35123456789012345678",
  "payoutMethod": "BANK"
}
```

### MoR Vendor Requesting Payout

```bash
POST /vendors/mor/request-payout
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "amount": 50000,
  "currency": "EUR",
  "description": "Monthly payout request"
}
```

### Checking Payment Account Status

```bash
GET /vendors/payment/status
Authorization: Bearer <jwt_token>

# Response for MoR vendor:
{
  "isReady": true,
  "hasAccount": true,
  "provider": "MOR",
  "payoutModel": "MOR",
  "balance": 125000,
  "manualPayoutMethod": "BANK",
  "hasBankDetails": true,
  "country": "RS",
  "stripeSupported": false,
  "message": "Your payment account is ready to receive payments."
}
```

## Compliance & Legal Considerations

### MoR Model Compliance

- **Platform as Seller**: In MoR model, the platform is the official seller to customers
- **Vendor as Supplier**: Vendors act as suppliers to the platform
- **Invoicing**: 
  - Vendor invoices the platform for goods/services
  - Platform invoices the customer
- **Tax Handling**: Platform is responsible for tax collection and remittance
- **Payouts**: Treated as supplier payments (not marketplace payouts)

### Stripe Connect Compliance

- **Direct Relationship**: Vendor has direct relationship with Stripe
- **KYC/AML**: Stripe handles all compliance requirements
- **Tax**: Vendor is responsible for their own tax obligations
- **Payouts**: Stripe handles payouts directly to vendor

## Future Enhancements

1. **Admin Interface**: Build admin dashboard for processing MoR payouts
2. **Automated Payouts**: Integrate with banking APIs for automated transfers
3. **Invoice Generation**: Automatic invoice generation for MoR vendors
4. **Balance Alerts**: Notify vendors when balance reaches threshold
5. **Payout Scheduling**: Allow vendors to schedule recurring payouts
6. **Multi-Currency Support**: Enhanced currency handling for MoR balances
7. **Audit Trail**: Enhanced reporting and audit capabilities

