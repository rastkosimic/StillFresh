# MoR Payout Management Architecture

## Overview

The MoR (Merchant of Record) payout management has been refactored to follow a hybrid approach where:
- **Vendor data** (balances, transactions, payouts) remains in `vendor-service`
- **Admin endpoints** for viewing and managing payouts are in `payment-service`
- **Communication** between services uses Kafka events for loose coupling

## Architecture

### Service Responsibilities

#### vendor-service
- **Owns**: MoR balance storage, balance transactions, payout requests (all vendor-specific data)
- **Provides**: Vendor-facing endpoints (balance, transactions, payouts)
- **Handles**: Kafka listeners for MoR payout data requests and status updates
- **Publishes**: MoR payout data responses and status update responses

#### payment-service
- **Owns**: Admin endpoints for MoR payout management
- **Provides**: Admin-facing endpoints for viewing and managing payouts
- **Handles**: Kafka listeners for MoR payout responses
- **Publishes**: MoR payout data requests and status update requests

### Kafka Event Flow

#### Data Request Flow
1. Admin calls endpoint in `payment-service` (e.g., `GET /admin/mor/payouts/pending`)
2. `MoRPayoutService` creates a `MoRPayoutDataRequestEvent` and publishes it to Kafka
3. `MoRPayoutRequestListener` in `vendor-service` receives the event
4. `VendorService` processes the request and generates response data
5. `VendorEventPublisher` publishes `MoRPayoutDataResponseEvent` to Kafka
6. `MoRPayoutResponseListener` in `payment-service` receives the response
7. `MoRPayoutService` completes the `CompletableFuture` and returns data to admin endpoint

#### Status Update Flow
1. Admin calls endpoint in `payment-service` (e.g., `PUT /admin/mor/payouts/{id}/status`)
2. `MoRPayoutService` creates a `MoRPayoutStatusUpdateRequestEvent` and publishes it to Kafka
3. `MoRPayoutRequestListener` in `vendor-service` receives the event
4. `VendorService` updates the payout status in the database
5. `VendorEventPublisher` publishes `MoRPayoutStatusUpdateResponseEvent` to Kafka
6. `MoRPayoutResponseListener` in `payment-service` receives the response
7. `MoRPayoutService` completes the `CompletableFuture` and returns success to admin endpoint

## Kafka Topics

### Request Topics (payment-service → vendor-service)
- `mor-payout-data-request`: Requests for payout data (pending payouts, vendor balances, order payments, summary, vendor payouts)
- `mor-payout-status-update-request`: Requests to update payout status

### Response Topics (vendor-service → payment-service)
- `mor-payout-data-response`: Responses with payout data
- `mor-payout-status-update-response`: Responses confirming status updates

## New Components

### Shared Entities (shared-entities)
- `MoRPayoutDataRequestEvent`: Request for payout data
- `MoRPayoutDataResponseEvent`: Response with payout data
- `MoRPayoutStatusUpdateRequestEvent`: Request to update payout status
- `MoRPayoutStatusUpdateResponseEvent`: Response confirming status update

### Payment Service
- `MoRPayoutService`: Service for managing MoR payouts via Kafka
- `AdminController`: REST controller with admin endpoints for MoR payout management
- `MoRPayoutResponseListener`: Kafka listener for MoR payout responses
- `PaymentEventPublisher`: Extended with methods to publish MoR payout requests

### Vendor Service
- `MoRPayoutRequestListener`: Kafka listener for MoR payout requests
- `VendorEventPublisher`: Extended with methods to publish MoR payout responses

## Admin Endpoints (payment-service)

All endpoints are under `/admin/mor` and require `ADMIN` or `SUPER_ADMIN` role:

- `GET /admin/mor/payouts/pending` - Get all pending MoR payouts
- `GET /admin/mor/vendors/balances` - Get all MoR vendors with balances
- `GET /admin/mor/transactions/orders` - Get all MoR order payments (with optional date filtering)
- `GET /admin/mor/payouts/summary` - Get MoR payout summary statistics
- `PUT /admin/mor/payouts/{payoutId}/status` - Update payout status
- `GET /admin/mor/vendors/{vendorId}/payouts` - Get payouts for a specific MoR vendor

## Benefits

1. **Loose Coupling**: Services communicate via Kafka events, not direct HTTP calls
2. **Scalability**: Each service can scale independently
3. **Data Locality**: Vendor data stays in vendor-service where it belongs
4. **Separation of Concerns**: Payment processing logic in payment-service, vendor data in vendor-service
5. **Future Automation**: Easy to add automated payout processing in payment-service without touching vendor-service

## Configuration

Kafka topics are configured in `application.yml`:

### payment-service
```yaml
payment:
  topic:
    mor-payout-data-request: mor-payout-data-request
    mor-payout-data-response: mor-payout-data-response
    mor-payout-status-update-request: mor-payout-status-update-request
    mor-payout-status-update-response: mor-payout-status-update-response
```

### vendor-service
```yaml
payment:
  topic:
    mor-payout-data-request: mor-payout-data-request
    mor-payout-data-response: mor-payout-data-response
    mor-payout-status-update-request: mor-payout-status-update-request
    mor-payout-status-update-response: mor-payout-status-update-response
```

## Timeout Handling

The `MoRPayoutService` uses a 10-second timeout for Kafka request-response operations. If a response is not received within this time, the operation fails with an error message.

## Error Handling

- If vendor-service fails to process a request, it sends an error response with `success=false` and an error message
- If payment-service doesn't receive a response within the timeout, it throws a `RuntimeException`
- All errors are logged and returned to the admin as appropriate HTTP status codes

