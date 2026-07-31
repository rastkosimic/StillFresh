## Context

You are an AI coding agent working on the StillFresh Android app.
The backend vendor-service now exposes two **image upload endpoints** that accept a multipart file and return a stable URL. Both require a valid vendor JWT.

---

## Backend Endpoints

### Endpoint 1 — Vendor Profile Image

```
POST /vendors/upload-profile-image
Content-Type: multipart/form-data
Authorization: Bearer <vendor_jwt>

Field name: "image"  (the image file)
```

**Success response (200):**
```json
{ "imageUrl": "http://<host>/vendors/uploads/vendors/<uuid>.jpg" }
```

**Usage:** Call this during vendor profile completion. Take the returned `imageUrl` and pass it as the `imageUrl` field in the subsequent `PUT /vendors/update-profile` request body.

---

### Endpoint 2 — Offer Image

```
POST /images/upload
Content-Type: multipart/form-data
Authorization: Bearer <vendor_jwt>

Field name: "image"  (the image file)
```

**Success response (200):**
```json
{ "imageUrl": "http://<host>/vendors/uploads/offers/<uuid>.jpg" }
```

**Usage:** Call this when a vendor creates or edits an offer. Take the returned `imageUrl` and include it as the `imageUrl` field in the `POST /vendors/offer-create` or `POST /vendors/update-offer/{offerId}` request body.

---

## Important Notes

- Both endpoints accept `multipart/form-data` with the field named **`image`** (not `file`).
- The backend accepts JPEG, PNG, GIF, and WebP. Max file size is **10 MB**.
- The returned `imageUrl` is a fully-qualified URL (includes host). Store it as-is — it is publicly accessible without authentication (for display purposes).
- The upload step is **separate** from the profile/offer save step. Upload first, get the URL, then save.
- Compress or resize the image on the Android side before uploading to stay well under the 10 MB limit.

---

## High-level Implementation Goals (Android)

1. **Add a reusable image upload utility** that picks an image from the gallery (or camera), optionally compresses it, uploads it via multipart to the appropriate endpoint, and returns the URL.
2. **Wire endpoint 1** into the vendor profile completion screen so the vendor can set a profile photo.
3. **Wire endpoint 2** into the offer creation/editing screen so the vendor can attach an image to an offer.
4. **Display uploaded images** using the returned URL (Glide, Coil, or Picasso — whichever is already used in the project).

---

## Prompts for the AI Agent (Android Implementation)

Copy and paste these prompts one by one into the Android AI assistant.

---

### 1. Retrofit API interface — image upload

> **Prompt:**
> In the Android networking layer (Retrofit or equivalent), add two new API methods for image upload:
>
> 1. `POST /vendors/upload-profile-image` — multipart, field name `image`, returns `ImageUploadResponse(imageUrl: String)`.
> 2. `POST /images/upload` — multipart, field name `image`, returns `ImageUploadResponse(imageUrl: String)`.
>
> Both require the bearer token (reuse the existing auth interceptor).
> Use `@Multipart` and `@Part("image") MultipartBody.Part` in the Retrofit interface.
> Add the `ImageUploadResponse` data class if it does not already exist: `data class ImageUploadResponse(val imageUrl: String)`.
> Show me the updated Retrofit service interface and data class.

---

### 2. Image upload utility / helper

> **Prompt:**
> Create a reusable `ImageUploadHelper` (or similar) class/object in the Android app that:
>
> - Accepts a `Uri` of a local image (from the gallery or camera).
> - Compresses the image to JPEG quality 80 and resizes it so the longest side is at most 1024 px (use `BitmapFactory` + `Bitmap.compress`, or the image compression library already used in the project).
> - Converts the compressed bytes to a `MultipartBody.Part` with the part name `"image"` and the MIME type `image/jpeg`.
> - Exposes two suspend functions (or RxJava/callback equivalents consistent with the project's async pattern):
>   - `uploadProfileImage(uri: Uri): Result<String>` — calls `POST /vendors/upload-profile-image`, returns the `imageUrl` string on success.
>   - `uploadOfferImage(uri: Uri): Result<String>` — calls `POST /images/upload`, returns the `imageUrl` string on success.
> - Returns a `Result.failure` with a descriptive exception on network error or non-2xx response.
>
> Reuse the existing Retrofit instance and auth interceptor. Show me the full implementation.

---

### 3. Vendor profile image — UI wiring

> **Prompt:**
> In the vendor profile completion screen (or wherever the vendor sets their profile photo):
>
> - Add an image picker trigger (tap on avatar/placeholder → open gallery via `ActivityResultContracts.GetContent` with MIME type `image/*`).
> - When the vendor selects an image:
>   1. Show a loading indicator.
>   2. Call `ImageUploadHelper.uploadProfileImage(uri)`.
>   3. On success: display the image locally (using the URI or the returned URL) and store the `imageUrl` string in the ViewModel / form state so it is included in the subsequent `PUT /vendors/update-profile` request body as the `imageUrl` field.
>   4. On failure: dismiss the loader and show a snackbar/toast with the error message.
> - If the vendor has an existing `imageUrl` on their profile, pre-load it into the avatar view using Glide/Coil on screen open.
>
> Show me the ViewModel changes, Fragment/Activity changes, and any layout adjustments needed.

---

### 4. Offer image — UI wiring

> **Prompt:**
> In the offer creation and offer editing screens:
>
> - Add an image picker area (a rectangular placeholder with a camera/gallery icon).
>   - Tap → open gallery via `ActivityResultContracts.GetContent` with MIME type `image/*`.
> - When the vendor selects an image:
>   1. Show a loading indicator over the picker area.
>   2. Call `ImageUploadHelper.uploadOfferImage(uri)`.
>   3. On success: display a thumbnail of the uploaded image and store the `imageUrl` string in the ViewModel / form state so it is included in the `POST /vendors/offer-create` or `POST /vendors/update-offer/{offerId}` request body as the `imageUrl` field.
>   4. On failure: dismiss the loader and show a snackbar/toast with the error message.
> - If editing an existing offer that already has an `imageUrl`, pre-load that image into the picker area using Glide/Coil.
> - The image field is optional — if the vendor does not select an image, send `imageUrl: null` (or omit the field) in the offer payload.
>
> Show me the ViewModel changes, Fragment/Activity changes, and any layout adjustments needed.

---

### 5. Error handling & edge cases

> **Prompt:**
> Review the image upload flow in the Android app and add handling for the following edge cases:
>
> - **File too large**: If the compressed image is still above 9 MB (check `ByteArray.size` before uploading), show an error dialog telling the vendor to choose a smaller image. Do not attempt the upload.
> - **No internet**: The Retrofit call will throw an `IOException`. Catch it and show "No internet connection. Please try again."
> - **401 Unauthorized**: The vendor's token may have expired. Handle the same way as other 401 responses in the app (e.g., redirect to login or trigger a token refresh via the existing refresh mechanism).
> - **413 / 400 from server**: Show "Image could not be uploaded. Please try a different image."
> - **User cancels the picker**: No action needed — keep the previous state.
> - **Slow upload UX**: Show a `CircularProgressIndicator` or progress bar during upload. Disable the "Save" / "Create Offer" button until the upload resolves so the vendor cannot submit the form with a pending upload.
>
> Show me the updated error-handling code for both upload flows.
