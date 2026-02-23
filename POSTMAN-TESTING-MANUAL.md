# AutoClaim-AI – Complete Postman Testing Manual

This guide takes you from zero to a successful claim response with a calculated price. All requests go through the **Gateway** on port **8080**.

---

## Before You Start

1. **Start all 4 services** (Auth 8081, Pricing 8083, Claim 8085, Gateway 8080).
2. **PostgreSQL** must be running with databases: `auth_db`, `claim_db`, `pricing_db`.
3. **Insert at least one part price** in `pricing_db` so the claim can calculate a cost (see section 5 below).

---

## Step 1: Register a User

**Goal:** Create an account so you can log in and get a JWT.

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/register` |

**Body:**
- Type: **raw** → **JSON**
- Content:

```json
{
  "username": "john",
  "password": "secret123"
}
```

**What to expect:**
- **Status code:** `201 Created`
- **Body:** Plain text like `"User registered"` or similar success message.

If you get **4xx** or **5xx**, check: URL uses port **8080** (Gateway), Content-Type is JSON, and Auth service is running.

---

## Step 2: Login and Get the Token

**Goal:** Get a JWT token to use for protected endpoints.

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/login` |

**Body:**
- Type: **raw** → **JSON**
- Content (same username/password as register):

```json
{
  "username": "john",
  "password": "secret123"
}
```

**What to expect:**
- **Status code:** `200 OK`
- **Body (example):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwicm9sZSI6IlJPTEVfVVNFUiIsImlhdCI6MTcwODAwMDAwMCwiZXhwIjoxNzA4MDg2NDAwfQ.xxxxx"
}
```

**Where to copy the token:**
- Copy **only the value** of the `"token"` field (the long string in quotes).
- Do **not** include the word `"token"` or the quotes in the Authorization header—only the token string itself.

---

## Step 3: Set the Authorization Header (Exact Format)

For **any** request that requires authentication (e.g. Create Claim, Get All Claims), you must send the JWT in the header.

In Postman:

1. Open the request (e.g. Create Claim).
2. Go to the **Headers** tab.
3. Add a header:

| Key | Value |
|-----|--------|
| **Authorization** | `Bearer eyJhbGciOiJIUzI1NiJ9...` |

**Rules:**
- **Key:** exactly `Authorization` (capital A, rest lowercase is fine in HTTP).
- **Value:** the word **`Bearer`** (with a space after it) + **one space** + **your token** (no quotes).
- Example: `Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIi4uLn0.xxxx`

**Wrong (will cause 401):**
- `eyJhbGciOiJIUzI1NiJ9...`  ← missing `Bearer `
- `Bearer Bearer eyJ...`      ← double "Bearer"
- `"Bearer eyJ..."`           ← no quotes in the header value

---

## Step 4: Create a Claim

**Goal:** Send a photo URL, let the AI detect damaged parts, get prices from Pricing service, and receive a claim with a calculated total.

You can use **either** Query Params **or** JSON Body. Use **one** of the two options below.

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/claims/create` |
| **Headers** | `Authorization: Bearer <your-token>` (see Step 3) |

### Option A: JSON Body (recommended)

1. **Body** tab → **raw** → **JSON**.
2. Use this (replace with your username and, if you want, another image URL):

```json
{
  "username": "john",
  "photoUrl": "https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=600"
}
```

- **username:** must match the user you registered (e.g. `john`).
- **photoUrl:** a public image URL. The one above is a real public image (car/crash-related). You can also try:
  - `https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=600` (car)
  - Any other **public** image URL of a damaged car.

### Option B: Query params (URL parameters)

- **URL:**  
  `http://localhost:8080/api/claims/create?username=john&photoUrl=https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=600`
- **Body:** none (or leave empty).
- **Note:** If the image URL contains `?` or `&`, Postman may interpret it as part of the query. In that case, use **Option A (JSON body)** to avoid encoding issues.

**What to expect:**
- **Status code:** `200 OK`
- **Body:** A JSON object representing the claim, for example:

```json
{
  "id": 1,
  "username": "john",
  "photoUrl": "https://...",
  "damagedParts": ["Hood", "Front Bumper", "Fender"],
  "totalCost": 1250.50,
  "jsonData": "...",
  "createdAt": "2025-02-23T..."
}
```

- **damagedParts:** list of parts the AI detected from the image.
- **totalCost:** sum of prices from the Pricing service for those parts.

---

## Step 5: Check PostgreSQL `pricing_db` So the AI-Detected Part Has a Price

The Claim service calls the Pricing service for **each** part in `damagedParts`. The Pricing service looks up the part name in the table **`part_prices`** in the **`pricing_db`** database.

**Table:** `part_prices`  
**Columns:** `id`, `part_name`, `price`

**What to check:**

1. Connect to PostgreSQL and use database `pricing_db`:
   ```sql
   \c pricing_db
   ```

2. List all part prices:
   ```sql
   SELECT * FROM part_prices;
   ```

3. **Match part names:**  
   The AI might return names like `Hood`, `Front Bumper`, `Fender`, `Door`, etc. (letters and spaces; no extra symbols).  
   The Pricing service looks up by **exact** `part_name`. So:
   - If the AI returns **"Hood"**, you must have a row with `part_name = 'Hood'`.
   - If the AI returns **"Front Bumper"**, you need `part_name = 'Front Bumper'`.

4. **Insert example data** so at least one detected part has a price:
   ```sql
   INSERT INTO part_prices (part_name, price) VALUES ('Hood', 500.00);
   INSERT INTO part_prices (part_name, price) VALUES ('Front Bumper', 350.00);
   INSERT INTO part_prices (part_name, price) VALUES ('Fender', 400.00);
   INSERT INTO part_prices (part_name, price) VALUES ('Door', 600.00);
   ```

5. If a detected part has **no** row in `part_prices`, the Pricing service returns `0.0` for that part (see `PricingService.getPrice`). So:
   - **totalCost can be 0** if none of the detected parts exist in `part_prices`.
   - To get a **non-zero totalCost**, ensure the strings in `damagedParts` match `part_name` in `part_prices` (case-sensitive).

---

## Step 6: Optional – Get All Claims

| Field | Value |
|-------|--------|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/claims/all` |
| **Headers** | `Authorization: Bearer <your-token>` |

**Expected:** `200 OK` and a JSON array of all claims.

---

## Troubleshooting – Common Mistakes

| Problem | Cause | Fix |
|--------|--------|-----|
| **401 Unauthorized** on Create Claim or Get All Claims | Missing or invalid token | Add header **Key:** `Authorization`, **Value:** `Bearer <token>` (one space after "Bearer"). Copy token from Login response only. |
| **401** even with a token | Word "Bearer" missing | Value must be exactly `Bearer ` + space + token, e.g. `Bearer eyJhbG...`. |
| **403 Forbidden** | Token invalid or expired | Login again and use the new token. |
| **400 Bad Request** on Create Claim | Missing or wrong body/params | Send either JSON body with `username` and `photoUrl`, or query params `username=...&photoUrl=...`. Both must be non-empty. |
| **Connection refused** or **Could not get response** | Wrong port or service not running | Use **8080** for all Postman requests (Gateway). Ensure all 4 services are running (8080, 8081, 8083, 8085). |
| **404** on Register/Login | Wrong path or port | URL must be `http://localhost:8080/api/auth/register` and `http://localhost:8080/api/auth/login` (port 8080, not 8081). |
| **totalCost is 0** in claim response | No matching part in `pricing_db` | Check `part_prices` table; insert rows whose `part_name` matches what the AI returns (e.g. "Hood", "Front Bumper"). |
| **500** or claim with error in `jsonData` | Gemini API key or network issue | Check `claim-service` `application.yaml` for `gemini.api.key` and that the service can reach the Gemini API. |

---

## Quick Checklist: Zero → Successful Claim

1. Start PostgreSQL; create `auth_db`, `claim_db`, `pricing_db`.
2. Insert part prices into `pricing_db.part_prices` (e.g. Hood, Front Bumper).
3. Start Auth (8081), Pricing (8083), Claim (8085), Gateway (8080).
4. **Postman – Register:** `POST http://localhost:8080/api/auth/register` with JSON `{"username":"john","password":"secret123"}` → expect **201**.
5. **Postman – Login:** `POST http://localhost:8080/api/auth/login` with same JSON → copy the **token** from the response.
6. **Postman – Create Claim:** `POST http://localhost:8080/api/claims/create` with header `Authorization: Bearer <token>` and body `{"username":"john","photoUrl":"https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=600"}` → expect **200** and a claim with `damagedParts` and `totalCost`.

If any step fails, use the Troubleshooting table above and double-check port **8080**, the exact **Authorization** format, and that `part_name` in `pricing_db` matches the AI-detected part names.
