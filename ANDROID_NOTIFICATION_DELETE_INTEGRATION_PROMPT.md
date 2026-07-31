# Android Notification Delete Integration Prompt

## Purpose

This prompt is for an AI agent implementing the **notification delete** feature in the StillFresh Android app. The backend supports **soft delete**: when the user "deletes" a notification in the app, the server marks it as deleted so it no longer appears in the notification list. Implement the UI and API call so the user can remove notifications from their inbox.

---

## Overview

- **User action:** User deletes a notification (e.g. swipe to delete, trash icon, or long-press menu).
- **Backend behavior:** Soft delete (notification is marked deleted; it disappears from list endpoints).
- **App responsibility:** Call the delete API, then remove the item from the local list (or refresh the list) so the UI updates immediately.

---

## API Contract

### Delete a notification (soft delete)

**Endpoint:**
```
DELETE /api/notifications/{notificationId}
```

**Base URL:** Use the app’s configured API base URL (e.g. gateway). Example:
- Development: `http://10.0.2.2:8080` (emulator) or your dev host
- Production: `https://your-api-gateway-domain`

**Full path example:** `DELETE {baseUrl}/api/notifications/550e8400-e29b-41d4-a716-446655440000`

**Headers:**
- `Authorization: Bearer <jwt-token>` (required)

**Path parameter:**
- `notificationId` (String, required) – UUID of the notification to delete (same `id` returned in the notification list).

**Success response (200 OK):**
```json
{
  "success": true,
  "message": "Notification deleted",
  "data": null,
  "error": null
}
```

**Error responses:**
- **400 BAD REQUEST** – Notification not found or not owned by the current user.
  - Response body typically includes `"message": "Notification not found or access denied"` or similar.
- **401 UNAUTHORIZED** – Missing or invalid JWT. Redirect to login if needed.
- **500 INTERNAL SERVER ERROR** – Server error; show a generic error and optionally retry.

**Idempotency:** Deleting an already-deleted notification returns 400 (not found). The app can treat that as “already gone” and remove the item from the list if it was still visible.

---

## Implementation Requirements

### 1. Where to expose “Delete”

- **Notification list screen** (inbox): Each item should support delete.
- **Optional:** Notification detail screen (if you have one) with a delete action.

Choose one or more of the following patterns (or equivalent):

- **Swipe-to-dismiss:** Swipe a list item to reveal a “Delete” action (or use `ItemTouchHelper` / `RecyclerView` swipe).
- **Long-press context menu:** Long-press on a notification item shows a menu with “Delete” (and optionally “Mark as read”).
- **Trash/delete icon:** A delete icon on each list item or in an overflow menu.

### 2. API call

- **Method:** `DELETE`.
- **URL:** `{baseUrl}/api/notifications/{notificationId}`.
- **Headers:** `Authorization: Bearer <token>`. Use the same token/session the app uses for other authenticated calls.
- **No request body.**

Use the notification’s `id` (UUID string) from the list payload as `notificationId`. Handle 200 as success and 400/401/5xx as described above.

### 3. UI behavior after delete

- **On success (200):**
  - Remove the deleted notification from the in-memory/list adapter so it disappears from the list immediately (recommended).
  - Alternatively, refresh the notification list from the server; the deleted item will no longer be returned (backend filters by `deleted = false`).
- **On 400 (not found / access denied):**
  - Remove the item from the list anyway (treat as “already deleted”), or show a short message like “Notification no longer available.”
- **On 401:** Redirect to login or refresh token as per app’s auth flow.
- **On 5xx or network error:** Show a retry or “Something went wrong” message; do not remove the item unless the user retries and gets success.

### 4. Loading and errors

- While the delete request is in progress, show a loading state (e.g. disable the delete action or show a progress indicator on that item).
- On failure (other than 400 treated as “already gone”), show a Snackbar or Toast so the user knows the delete did not succeed.
- Optional: “Undo” is not supported by the API (soft delete is final for the inbox); do not promise undo unless you add a dedicated “restore” API later.

### 5. Data model and list source

- Notifications shown in the list should come from the existing **GET** list endpoint(s) (e.g. “get my notifications” or “get unread”). The backend only returns non-deleted notifications, so after a successful delete and list refresh, the item will not reappear.
- Ensure the list item’s primary key or identifier used for delete is the notification `id` (UUID) from the server.

---

## Checklist for the AI agent

- [ ] Add a delete action (swipe, long-press, or icon) on the notification list (and optionally on detail).
- [ ] Implement `DELETE /api/notifications/{notificationId}` with correct base URL and `Authorization: Bearer <token>`.
- [ ] Use the notification’s `id` (UUID) from the list as `notificationId`.
- [ ] On 200: remove the item from the list (or refresh list) so it disappears.
- [ ] On 400: remove from list or show “not available”; on 401 handle auth; on 5xx/network show error and do not remove.
- [ ] Show loading during the request and a user-visible error on failure (except when treating 400 as “already gone”).

---

## Backend context (for reference)

- Delete is **soft delete**: the row is marked `deleted = true` and excluded from all list endpoints.
- Only the owning user can delete a notification (enforced by backend).
- No “restore” or “undelete” endpoint is described here; the inbox only shows non-deleted items.

Use this document as the single source of truth for integrating the notification delete feature in the Android app.
