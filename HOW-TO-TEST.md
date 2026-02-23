# How to Test AutoClaim-AI (Postman via Gateway)

**Important:** Use **only port 8080** (Gateway) for all requests. Do not call 8081, 8083, or 8085 directly from Postman.

---

## Prerequisites

- PostgreSQL running with databases: `auth_db`, `claim_db`, `pricing_db` (user: `postgres`, password: `p1234`).
- At least one part price in `pricing_db`:  
  `INSERT INTO part_prices (part_name, price) VALUES ('Hood', 500), ('Front Bumper', 350), ('Fender', 400);`
- All 4 services running: Gateway (8080), Auth (8081), Pricing (8083), Claim (8085).

---

## 1. Register

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/register` |
| **Headers** | `Content-Type: application/json` |
| **Body** (raw → JSON) | `{"username": "john", "password": "secret123"}` |

**Expected:** Status **201 Created**.

---

## 2. Login (get the token)

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/auth/login` |
| **Headers** | `Content-Type: application/json` |
| **Body** (raw → JSON) | `{"username": "john", "password": "secret123"}` |

**Expected:** Status **200 OK**.  
**Copy the token:** In the response body, copy only the **value** of the `"token"` field (the long string). Example:  
`eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIi...`

---

## 3. Create Claim (with JWT)

| Field | Value |
|-------|--------|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/claims/create` |
| **Headers** | `Authorization: Bearer <paste-your-token>`  
| | `Content-Type: application/json` |
| **Body** (raw → JSON) | `{"username": "john", "photoUrl": "https://i.ebayimg.com/images/g/LPUAAOSwOkZjOdYh/s-l1600.jpg"}` |

**Expected:** Status **200 OK** with a JSON claim containing `damagedParts` (list of part names) and `totalCost` (sum of prices from Pricing service).

**Alternative – Query params:**  
URL: `http://localhost:8080/api/claims/create?username=john&photoUrl=https://i.ebayimg.com/images/g/LPUAAOSwOkZjOdYh/s-l1600.jpg`  
Header: `Authorization: Bearer <token>`  
Body: none.

---

## Summary: URLs (all via Gateway, port 8080)

- Register: `http://localhost:8080/api/auth/register`
- Login: `http://localhost:8080/api/auth/login`
- Create Claim: `http://localhost:8080/api/claims/create`

Use **Authorization: Bearer &lt;token&gt;** for Create Claim (and any other protected endpoint).
