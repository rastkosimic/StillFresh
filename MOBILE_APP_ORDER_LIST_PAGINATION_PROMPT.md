## Mobile app order list pagination changes

This document explains the **backend changes to the `/orders` endpoint** and what the **Android app** needs to do to support them. It is written for an AI coding agent working on the Android app.

### 1. Backend behavior summary

- **Path is unchanged**:  
  - `GET /orders`
  - Optional: `GET /orders?status=COMPLETED`

- **New query parameters (server-side pagination)**:
  - `page` (optional, 0-based index, default `0`)
  - `size` (optional, default `20`, capped at a maximum of `100` on the server)
  - Examples:
    - `GET /orders?page=0&size=20`
    - `GET /orders?page=1&size=20&status=COMPLETED`

- **Important: response shape changed**
  - Previously, the endpoint returned a **plain JSON array** of `Order` objects.
  - Now it returns a **Spring Data `Page<Order>` object**, with this structure:

```json
{
  "content": [
    { /* Order */ },
    { /* Order */ }
  ],
  "totalElements": 123,
  "totalPages": 7,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

- **Roles and semantics are the same**:
  - Regular users still only see **their own orders**.
  - `status` still filters by order status (e.g. `CANCELLED`, `COMPLETED`, etc.).
  - `page`/`size` simply control how many orders are returned and which slice of the result set is fetched.

### 2. Android networking changes

#### 2.1. API interface

If the Android app currently has something like:

```kotlin
@GET("orders")
suspend fun getOrders(
    @Query("status") status: String? = null
): List<OrderDto>
```

it must be updated to accept pagination parameters and to parse the **paged response wrapper** instead of a bare list.

**Target API signature:**

```kotlin
@GET("orders")
suspend fun getOrders(
    @Query("status") status: String? = null,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 20
): PageDto<OrderDto>
```

Where `PageDto<T>` matches the Spring `Page` JSON shape:

```kotlin
data class PageDto<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)
```

and `OrderDto` stays aligned with the existing backend `Order` representation.

> **Note:** Actual package names and DTO structures in the Android app may differ. Reuse the existing `OrderDto` used for orders; only wrap it in `PageDto`.

#### 2.2. Default behavior (first page)

- When the Android app **does not care about multiple pages yet**, it can:
  - Call `getOrders(status = ..., page = 0, size = DEFAULT_SIZE)`.
  - Use `response.content` as the list of orders to show.
  - Use `response.totalElements` or `response.last` to know if more pages exist.

### 3. UI / repository changes for pagination

The backend now supports efficient pagination; the UI should use it instead of assuming it receives the entire history.

#### 3.1. Central repository or use case layer

If orders are fetched through a repository/use-case, update it to expose paginated fetches, for example:

```kotlin
suspend fun fetchOrdersPage(
    status: String? = null,
    page: Int,
    size: Int
): PageDto<Order>
```

This repository should:
- Call the updated `getOrders` API.
- Map network DTOs to domain models.

#### 3.2. Screen behavior

Depending on how the app currently shows orders:

- **Simple screen (no “load more” yet)**:
  - On initial load, call `page = 0`, `size = DEFAULT_SIZE` (e.g. 20).
  - Display `content` from the response.
  - Optionally show a message if `content.isEmpty()` and `number == 0`.

- **Load-more / infinite-scroll behavior (recommended)**:
  - Keep track of current `page` and whether you have reached the last page (`response.last == true` or `page >= totalPages - 1`).
  - When the user scrolls near the bottom or presses “Load more”:
    - If not at last page, call with `page + 1`, same `size`, append `content` to the current list.
  - Prevent duplicate concurrent loads for the same next page.

#### 3.3. Status filtering

Status handling does not change conceptually, but pagination applies to filtered lists too:

- To get **completed orders only**, first page:

```kotlin
getOrders(
    status = "COMPLETED",
    page = 0,
    size = 20
)
```

- To get the **next page** of completed orders:

```kotlin
getOrders(
    status = "COMPLETED",
    page = nextPage,
    size = 20
)
```

### 4. Error handling and edge cases

The Android app should handle these situations gracefully:

- **Empty page on first load**:
  - `content` is empty, `number == 0`, `totalElements == 0`.
  - Show an empty-state UI (e.g. “You don’t have any orders yet.”).

- **Empty page on subsequent loads**:
  - Should normally coincide with `last == true` or `page >= totalPages`.
  - Stop requesting further pages.

- **Invalid page/size values**:
  - The backend clamps `size` internally, but the app should still:
    - Use non-negative `page` indices.
    - Use reasonable `size` (e.g. 20–50).

### 5. Backward compatibility notes

- If any part of the Android code still assumes the response is a **plain list**, it will break once the app starts using the updated backend, because the JSON is now a page object.
- All new or updated order-listing code should:
  - Parse the `PageDto<OrderDto>` shape.
  - Use `content` as the list and ignore or use metadata as needed.

### 6. Summary of required Android changes

For the AI Android agent, the concrete tasks are:

1. **Update Retrofit (or other HTTP client) interface** for `GET /orders` to:
   - Accept `status`, `page`, and `size` query parameters.
   - Return a `PageDto<OrderDto>` instead of `List<OrderDto>`.
2. **Introduce a `PageDto<T>` model** that matches the Spring `Page` JSON structure.
3. **Update the repository/use-case layer** to:
   - Request orders by page.
   - Expose `PageDto<Order>` (or equivalent) to the ViewModel.
4. **Update the orders UI screen(s)** to:
   - Use `content` for displaying orders.
   - Support either:
     - a single first page (simplest), or
     - a proper “load more” / infinite-scroll scheme using `page` and `last/totalPages`.
5. **Test both scenarios**:
   - No orders at all.
   - Many orders (ensure loading more pages works and does not freeze the app).

