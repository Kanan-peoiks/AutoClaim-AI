# Grand Reset & Fix – Exact Postman Requests

**Use only Gateway (port 8080)** for all requests.

---

## Prerequisites

- PostgreSQL: `auth_db`, `claim_db`, `pricing_db` (user: `postgres`, password: `p1234`)
- In `pricing_db`:  
  `INSERT INTO part_prices (part_name, price) VALUES ('Headlight', 200), ('Bumper', 350), ('Hood', 500), ('Door', 600), ('Fender', 400);`
- All 4 services running: Gateway 8080, Auth 8081, Pricing 8083, Claim 8085

---

## 1. Register

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/register` |
| **Headers** | `Content-Type: application/json` |
| **Body** | raw → JSON |

```json
{
  "username": "john",
  "password": "secret123"
}
```

**Expected:** Status **201 Created**, body e.g. `"User registered"`.

---

## 2. Login (copy the token)

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/login` |
| **Headers** | `Content-Type: application/json` |
| **Body** | raw → JSON |

```json
{
  "username": "john",
  "password": "secret123"
}
```

**Expected:** Status **200 OK**.  
**Copy the token:** In the response, copy only the **value** of `"token"` (the long string). Example:
`eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIi...`

---

## 3. Create Claim (with JWT + working image URL)

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/claims/create` |
| **Headers** | `Authorization: Bearer <paste-your-token>` |
| | `Content-Type: application/json` |
| **Body** | raw → JSON |

**Use this real working image URL (public car damage photo):**

```json
{
  "username": "john",
  "photoUrl": "https://images.unsplash.com/photo-1580273916550-e323be2ae537"
}
```

**Expected:** Status **200 OK** with a claim JSON containing:
- `damagedParts`: list of parts from [Headlight, Bumper, Hood, Door, Fender]
- `totalCost`: sum of prices from `pricing_db`

**If you get 400 "Invalid Image URL":** The photo URL could not be fetched (404, redirect, or blocked). Use the Unsplash URL above; if it still fails, try:
`https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/Car_crash_2.jpg/1200px-Car_crash_2.jpg`

---

## Summary

| Step | URL | Body |
|------|-----|------|
| Register | `POST http://localhost:8080/api/auth/register` | `{"username":"john","password":"secret123"}` |
| Login | `POST http://localhost:8080/api/auth/login` | `{"username":"john","password":"secret123"}` |
| Create Claim | `POST http://localhost:8080/api/claims/create` | `{"username":"john","photoUrl":"https://images.unsplash.com/photo-1580273916550-e323be2ae537"}` + Header: `Authorization: Bearer <token>` |

All requests go to **localhost:8080** (Gateway).
