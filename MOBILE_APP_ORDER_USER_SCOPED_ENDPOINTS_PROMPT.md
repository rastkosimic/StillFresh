# Mobile App: User-Scoped Order Endpoints

## Overview

The backend now **scopes order endpoints by the authenticated user**. When a **customer** calls the order API with a valid JWT, they receive **only their own orders**. Your app must send the JWT on every request and handle the documented response codes.

---

## Behavior Summary

| Endpoint | Customer (USER) | SUPER_ADMIN |
|----------|-----------------|--------------|
| **GET /orders** | Only that user's orders | All orders |
| **GET /orders?status=...** | Only that user's orders with that status | All orders with that status |
| **GET /orders/{id}** | Order only if it belongs to the user | Any order by ID |
| **PUT /orders/{id}/cancel** | Can cancel only own orders | Can cancel any order |

Authentication is required for all of the above. The backend reads the user from the JWT (via gateway headers).

---

## What You Must Do

1. **Send JWT on every order request**  
   Use the same auth pattern as elsewhere:  
   `Authorization: Bearer <access_token>`  
   so the gateway can set the user context. Without a valid token, the API returns **401**.

2. **GET /orders**  
   - **Success (200):** Response body is a JSON array of orders. For customers this is **only orders created by that user** (basket, cancelled, completed, expired, etc.).  
   - **401 Unauthorized:** Missing or invalid token. Prompt (re-)login.

3. **GET /orders/{id}**  
   - **200:** Order JSON (only if it belongs to the user, or if caller is SUPER_ADMIN).  
   - **401:** Not authenticated.  
   - **404:** Order not found or not owned by the user. Do not expose “forbidden” vs “not found”; show a generic “Order not found” or refresh the list.

4. **PUT /orders/{id}/cancel**  
   - **200:** Body like `{ "success": true, "message": "Order cancelled successfully" }`. Refresh the order list.  
   - **401:** Not authenticated.  
   - **403:** Order not found or not owned by the user. Body may contain `"message": "Order not found or access denied"`. Show a short error and do not retry cancel.  
   - **400:** Cancel not allowed (e.g. already completed/expired). Show the message from the response.

5. **No client-side filtering by userId**  
   The backend already returns only the current user’s orders for **GET /orders**. Do not filter again by `userId` on the client; rely on the API.

6. **Basket / lists**  
   Continue to split the list into basket (active), cancelled, realized, expired, etc. using the `status` field. The array from **GET /orders** (and **GET /orders?status=...**) is already restricted to the logged-in user.

---

## Response Codes (Quick Reference)

- **200** – Success; body contains order(s) or success payload.
- **400** – Bad request / cancel not allowed; show `message` if present.
- **401** – Unauthorized; (re-)authenticate and retry.
- **403** – Forbidden (e.g. cancel not allowed for this order); show generic error.
- **404** – Order not found or not accessible; show “Order not found” or refresh list.

Implement the above so the customer-facing app only ever sees and acts on their own orders, and handles 401/403/404 without leaking security details.
