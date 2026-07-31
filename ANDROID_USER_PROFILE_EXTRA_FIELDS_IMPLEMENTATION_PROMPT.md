## Context

You are an AI coding agent working on the StillFresh Android app.
The backend user-service has been extended with additional **optional profile fields** for the logged-in customer:

- `firstName`
- `lastName`
- `address`
- `country`
- `birthday` (ISO-8601 string `yyyy-MM-dd`)
- `dietaryPreference`

The user-service exposes:

- `GET /users` – returns the authenticated `User` object, now including all of the above fields.
- `PUT /users/profile/name` – body: `{ "firstName": "...", "lastName": "..." }`
- `PUT /users/profile/address` – body: `{ "address": "..." }`
- `PUT /users/profile/country` – body: `{ "country": "..." }`
- `PUT /users/profile/birthday` – body: `{ "birthday": "yyyy-MM-dd" }`
- `PUT /users/profile/dietary-preference` – body: `{ "dietaryPreference": "..." }`

All of these endpoints:

- Require the user to be authenticated (JWT bearer token, same as other secured calls).
- Operate on the **currently logged-in user**, inferred from the token (no `userId` in the path).
- Return the **updated `User` JSON** so the app can refresh its local state immediately.

Use this information and the prompts below to implement and wire the Android-side changes.

---

## High-level implementation goals (Android)

1. **Extend the local user/profile model** to include the new fields.
2. **Update API layer** (e.g. Retrofit interfaces, DTOs) to:
   - Fetch the full profile (`GET /users`) after login and when opening the profile screen.
   - Send one-off updates to the backend for each field via the new per-field `PUT` endpoints.
3. **Update profile UI** so that:
   - The profile screen displays all new fields.
   - Each field can be edited in its own flow/screen/bottom sheet, consistent with the existing UX.
4. **Handle validation and formatting**:
   - Keep fields optional (empty strings / null values allowed).
   - For `birthday`, use a date picker and format as `yyyy-MM-dd` when sending to the backend.
5. **Keep token handling consistent** with the existing networking stack (attach the same Authorization header that is used for other authenticated calls).

---

## Prompts for the AI agent (Android implementation)

You can copy-paste these prompts one by one into the Android AI assistant inside this project.

### 1. Data model and API DTOs

> **Prompt:**  
> In the Android app, locate the data model(s) and API DTO(s) that represent the logged-in user profile (e.g. `User`, `UserProfile`, or similar).  
> - Extend the user model to include the following **nullable** fields: `firstName`, `lastName`, `address`, `country`, `birthday` (String), `dietaryPreference`.  
> - Ensure the JSON field names match the backend (`firstName`, `lastName`, `address`, `country`, `birthday`, `dietaryPreference`).  
> - Show me the updated data classes and any required `@SerializedName` / Moshi / Kotlinx annotations.

### 2. Retrofit (or other HTTP client) interface updates

> **Prompt:**  
> Update the Android networking layer (Retrofit or equivalent) to support the new profile endpoints:  
> - `GET /users` → returns the full `User` object with the new fields.  
> - `PUT /users/profile/name` with body `{ "firstName": "...", "lastName": "..." }`.  
> - `PUT /users/profile/address` with body `{ "address": "..." }`.  
> - `PUT /users/profile/country` with body `{ "country": "..." }`.  
> - `PUT /users/profile/birthday` with body `{ "birthday": "yyyy-MM-dd" }`.  
> - `PUT /users/profile/dietary-preference` with body `{ "dietaryPreference": "..." }`.  
> Implement/extend the Retrofit service interface, request DTOs, and response mapping so that all methods return the updated `User`.  
> Assume the base URL and auth interceptor (for adding the bearer token) already exist; reuse the same pattern as other authenticated API calls.

### 3. Repository / use case layer

> **Prompt:**  
> In the repository/use-case layer that handles user profile data, add methods to:  
> - Fetch the current user profile from `GET /users`.  
> - Update name (`firstName`, `lastName`) via `PUT /users/profile/name`.  
> - Update address via `PUT /users/profile/address`.  
> - Update country via `PUT /users/profile/country`.  
> - Update birthday via `PUT /users/profile/birthday`.  
> - Update dietary preference via `PUT /users/profile/dietary-preference`.  
> Each method should:  
> - Make the appropriate network call.  
> - Update any local cache / in-memory store / `DataStore` / `Room` with the returned `User`.  
> - Expose a convenient result type to the ViewModel (e.g. Kotlin `Flow`, `suspend` functions with a sealed `Result`, etc.).  
> Show me the updated repository/use case code.

### 4. ViewModel and state integration

> **Prompt:**  
> In the ViewModel that powers the profile screen, integrate the new repository methods so that:  
> - On profile screen load (or `onResume`), the ViewModel fetches `GET /users` and exposes a `User` state including the new fields.  
> - For each edit action (name, address, country, birthday, dietary preference), expose a function like `onNameChanged(...)`, `onAddressChanged(...)`, etc.  
> - These functions call the appropriate repository update methods and update the ViewModel state with the returned `User`.  
> - Include basic loading/error state handling that can be reflected in the UI.  
> Show me the ViewModel changes.

### 5. UI changes – profile screen

> **Prompt:**  
> Update the profile screen UI (XML layouts or Jetpack Compose) to:  
> - Display the new fields (`firstName`, `lastName`, `address`, `country`, `birthday`, `dietaryPreference`).  
> - Use a UX pattern consistent with the existing design (e.g. list items / rows that show a label, current value, and a chevron or edit icon).  
> - When the user taps a field, navigate to a dedicated edit UI (screen, dialog, or bottom sheet).  
> - Show placeholder text like "Not set" when a field is null/empty.  
> Implement the UI bindings to the ViewModel state and demonstrate how each value is rendered.

### 6. UI changes – per-field edit screens

> **Prompt:**  
> For each profile field, create or update an edit flow:  
> - **Name**: screen/bottom sheet allowing editing of `firstName` and `lastName` together; on save, call the name update method.  
> - **Address**: text input; on save, call the address update method.  
> - **Country**: either a free-text field or a dropdown (depending on existing UX); on save, call the country update method.  
> - **Birthday**: use a date picker; store the selected date; format it as `yyyy-MM-dd` when calling the birthday update API.  
> - **Dietary preference**: free-text field (for now); on save, call the dietary preference update method.  
> On a successful update, close the edit UI and rely on the updated `User` from the backend to refresh the profile screen.  
> Show me the UI code and its interaction with the ViewModel.

### 7. Validation, error handling, and UX polish

> **Prompt:**  
> Add lightweight validation and UX feedback for the new fields:  
> - Allow fields to be empty (they are optional), but prevent obviously invalid input where appropriate (e.g. reject birthday strings that don't parse to `yyyy-MM-dd`).  
> - If an update call fails (network error or 4xx/5xx), show a non-intrusive error message (e.g. Snackbar / Toast) and keep the previous value.  
> - Show loading indicators while a profile update is in progress for a field (e.g. disable the Save button and show a spinner).  
> - Make sure date parsing/formatting for birthday is centralized and tested.  
> Provide code snippets showing how you handle these cases.

### 8. Testing and verification

> **Prompt:**  
> Add or update instrumentation/unit tests to cover:  
> - Parsing and serialization of the extended `User` model with the new fields.  
> - Repository methods hitting the correct endpoints with correct request bodies.  
> - ViewModel logic for loading profile data and updating each field.  
> - UI tests (if available) to verify that editing a field triggers the right API call and that the updated value appears on the profile screen.  
> Suggest a minimal but effective set of tests and provide examples.

---

## Acceptance checklist for the Android implementation

Use this checklist to verify that the Android integration is complete:

- [ ] After login, the app fetches `GET /users` and populates **all** profile fields, including the new ones.
- [ ] The profile screen visibly shows: name (first + last), address, country, birthday, and dietary preference.
- [ ] Each field can be edited in its own flow, and on save:
  - [ ] The appropriate `/users/profile/...` endpoint is called.
  - [ ] The app updates its local user state with the response.
  - [ ] The UI refreshes to show the new value.
- [ ] Birthday is correctly formatted as `yyyy-MM-dd` when sending to the backend, and parsed correctly when displaying.
- [ ] Empty or unset fields are handled gracefully with clear placeholder text.
- [ ] Error and loading states are handled gracefully, without leaving the UI in a broken state.

