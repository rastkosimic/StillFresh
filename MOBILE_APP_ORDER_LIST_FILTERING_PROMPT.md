## Mobile App AI Agent Prompt: Order List Filtering (Basket / Cancelled / Realized)

### Overview
You are an AI agent responsible for implementing **order list filtering** in the StillFresh Android app.  
Your goal is to show the user three clearly separated lists:
- **Basket (Current Reservations)** – active, not-yet-finished orders.
- **Cancelled Orders** – orders the customer or vendor has cancelled/rejected.
- **Realized Orders** – completed orders (picked up/fulfilled).

Orders have a `status` field in the backend. You must use this to decide **which orders appear in which section**.

---

### Base API & Authentication

- **Base URL**: API Gateway base URL (e.g. `https://api.stillfresh.app` or `http://localhost:8080` for local dev).
- **Order Endpoint (through gateway)**: `GET /orders`
- **Auth**: Every request must include the JWT token:

```http
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

If you get `401 Unauthorized`, treat the token as expired, log the user out and navigate to the login screen.

---

### Backend Filtering Capabilities

The order-service exposes the following backend behavior (via the gateway):

- **Endpoint**: `GET /orders`
- **Query Parameters**:
  - `status` (optional, string): if provided, backend returns **only orders with that status**.
    - Example: `GET /orders?status=CANCELLED` → only cancelled orders.
    - Example: `GET /orders?status=COMPLETED` → only realized orders.

If `status` is **not** provided, the backend returns **all orders** visible to the current user (typically filtered by user on the gateway side).

Valid statuses:
- `PENDING`
- `CONFIRMED`
- `PROCESSING`
- `READY`
- `COMPLETED`
- `CANCELLED`
- `EXPIRED` (pickup window passed; user not charged)

---

### Product Requirement: 3 Lists

You must implement three logical views for the user:

- **Basket / Reserved Orders (tab or section 1)**  
  - Show only **currently reserved / active** orders.  
  - These are orders that are **not finished yet** (do **not** include `EXPIRED`):
    - `PENDING`
    - `CONFIRMED`
    - `PROCESSING`
    - `READY`
  - **Exclude** `EXPIRED`, `CANCELLED`, and `COMPLETED` from the basket.

- **Cancelled Orders (tab or section 2)**  
  - Show only orders with `status = "CANCELLED"`. Optionally include `EXPIRED` here (or in a separate “Expired” section); see `MOBILE_APP_ORDER_EXPIRY_AND_REMINDERS_PROMPT.md`.

- **Realized Orders / Past Orders (tab or section 3)**  
  - Show only orders with `status = "COMPLETED"`.

You may implement these as **tabs**, a segmented control, or three separate screens, but data rules must be as above.

---

### Network Integration Strategy (Android)

Because the backend currently accepts a **single** `status` query parameter, you will combine:
- **Backend filtering** for single-status categories (cancelled, realized).
- **Client-side filtering** to group multiple “active” statuses into the basket list.

#### 1. Fetch All Orders Once

For simplicity and reduced network overhead, you can fetch all orders once and filter on the client:

- **Endpoint**: `GET /orders` (no query params)

From the response array, split into three lists in memory based on `status`.

#### 2. Or Fetch Per Category (for cancelled/realized)

As an optimization, you can fetch some categories already filtered on the backend:

- **Basket (active)**: `GET /orders` (then filter in app to active statuses)
- **Cancelled**: `GET /orders?status=CANCELLED`
- **Realized**: `GET /orders?status=COMPLETED`

Choose one of these strategies consistently and document it in the code.

---

### Status-Based Filtering Rules (App Logic)

When you have a list of orders (`List<OrderDto>`), derive three collections:

```kotlin
data class OrderDto(
    val id: Long,
    val offerId: Long,
    val userId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val vendorId: Long,
    val currency: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)

val activeStatuses = setOf("PENDING", "CONFIRMED", "PROCESSING", "READY")
// Do not include EXPIRED in basket
val basketOrders = allOrders.filter { it.status in activeStatuses }
val cancelledOrders = allOrders.filter { it.status == "CANCELLED" }
val realizedOrders = allOrders.filter { it.status == "COMPLETED" }
// Optional: val expiredOrders = allOrders.filter { it.status == "EXPIRED" }
```

You **must not** show cancelled, completed, or **expired** orders inside the **basket** list.

---

### Android Networking (Example with Retrofit)

#### API Interface

```kotlin
interface OrdersApi {

    @GET("orders")
    suspend fun getOrders(
        @Query("status") status: String? = null
    ): Response<List<OrderDto>>
}
```

#### Repository Layer

```kotlin
class OrdersRepository(
    private val api: OrdersApi
) {
    suspend fun getAllOrders(): List<OrderDto> {
        val response = api.getOrders()
        if (!response.isSuccessful) {
            throw Exception("Failed to load orders: ${response.code()}")
        }
        return response.body().orEmpty()
    }

    suspend fun getCancelledOrders(): List<OrderDto> {
        val response = api.getOrders(status = "CANCELLED")
        if (!response.isSuccessful) {
            throw Exception("Failed to load cancelled orders: ${response.code()}")
        }
        return response.body().orEmpty()
    }

    suspend fun getRealizedOrders(): List<OrderDto> {
        val response = api.getOrders(status = "COMPLETED")
        if (!response.isSuccessful) {
            throw Exception("Failed to load realized orders: ${response.code()}")
        }
        return response.body().orEmpty()
    }
}
```

#### ViewModel Example (Single Fetch + Client Filtering)

```kotlin
class OrdersViewModel(
    private val repository: OrdersRepository
) : ViewModel() {

    private val _basketOrders = MutableLiveData<List<OrderDto>>()
    val basketOrders: LiveData<List<OrderDto>> = _basketOrders

    private val _cancelledOrders = MutableLiveData<List<OrderDto>>()
    val cancelledOrders: LiveData<List<OrderDto>> = _cancelledOrders

    private val _realizedOrders = MutableLiveData<List<OrderDto>>()
    val realizedOrders: LiveData<List<OrderDto>> = _realizedOrders

    private val activeStatuses = setOf("PENDING", "CONFIRMED", "PROCESSING", "READY")

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val all = repository.getAllOrders()
                _basketOrders.value = all.filter { it.status in activeStatuses }
                _cancelledOrders.value = all.filter { it.status == "CANCELLED" }
                _realizedOrders.value = all.filter { it.status == "COMPLETED" }
            } catch (e: Exception) {
                // TODO: expose error state to UI
            }
        }
    }
}
```

---

### UI / UX Requirements

- **Basket Tab**:
  - Show only `basketOrders`.
  - Display clear message when list is empty: “You have no active reservations.”

- **Cancelled Tab**:
  - Show only `cancelledOrders`.
  - Consider grouping by date and showing cancel reason when available.

- **Realized Tab**:
  - Show only `realizedOrders`.
  - Provide entry point to rate the vendor/offer if rating feature exists.

Reload lists after relevant actions:
- After placing a new order → refresh and show in Basket.
- After cancelling/rejecting an order → it must disappear from Basket and appear in Cancelled.
- After marking an order as completed → move from Basket to Realized.

---

### Error Handling & Edge Cases

- If the backend returns an empty list:
  - Show an empty state UI, not a generic error.
- If network request fails:
  - Show a retry button and a clear error message.
- Ensure that status comparison is **case-sensitive** using the exact string values from the backend.
- When merging data from multiple requests, avoid duplicates by using the order `id` as a unique key.

---

### What You Must Implement

1. **Networking**: A Retrofit (or equivalent) API client that calls `GET /orders` with optional `status` query parameter.
2. **Repository & ViewModel**: Logic to:
   - Fetch orders.
   - Split into basket / cancelled / realized lists based on `status`.
3. **UI**: Three tabs or sections that show the three lists and update after order placement, cancellation, or completion.
4. **Error & Empty States**: Proper user feedback for network errors and empty lists.

Follow these rules strictly so that:
- **Basket** shows only current reservations.
- **Cancelled** shows only cancelled orders.
- **Realized** shows only completed orders.


