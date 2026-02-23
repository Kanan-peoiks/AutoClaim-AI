# How to Run AutoClaim-AI

## Prerequisites

- **Java 17**
- **PostgreSQL** (running on `localhost:5432`)
- **Gradle** (or use the included `gradlew`)

---

## 1. Create PostgreSQL databases

Connect to PostgreSQL and create the three databases (or use `psql`):

```sql
CREATE DATABASE auth_db;
CREATE DATABASE claim_db;
CREATE DATABASE pricing_db;
```

Default DB user/password in the app: **postgres** / **p1234**.  
If your PostgreSQL user or password is different, update the `application.yaml` in each service (`auth-service`, `claim-service`, `pricing-service`).

---

## 2. Start the services (in any order, but Gateway last)

Open **4 terminals** in the project root `AutoClaim-AI`. Start each service in its own terminal.

**Terminal 1 – Auth Service (port 8081)**  
```bash
cd auth-service
../gradlew bootRun
```
Or from repo root:  
`./gradlew :auth-service:bootRun` (if subprojects are included in `settings.gradle`).

**Terminal 2 – Pricing Service (port 8083)**  
```bash
cd pricing-service
../gradlew bootRun
```

**Terminal 3 – Claim Service (port 8085)**  
```bash
cd claim-service
../gradlew bootRun
```

**Terminal 4 – Gateway (port 8080)**  
```bash
cd gateway-service
../gradlew bootRun
```

If your root `settings.gradle` does not include these modules, run from the **project root** and use the full path to each service’s `build.gradle`, or run `gradlew bootRun` from inside each service folder (e.g. `auth-service`, then `gateway-service`, etc.) as above.

Wait until each app logs something like “Started … Application” before using the API.

---

## 3. Use the app (all requests go through the Gateway on port 8080)

### Option A: Postman / Insomnia

**1) Register**  
- Method: `POST`  
- URL: `http://localhost:8080/api/auth/register`  
- Body: raw JSON  
```json
{
  "username": "john",
  "password": "secret123"
}
```

**2) Login**  
- Method: `POST`  
- URL: `http://localhost:8080/api/auth/login`  
- Body: raw JSON  
```json
{
  "username": "john",
  "password": "secret123"
}
```  
- Copy the **token** from the response (e.g. `"token": "eyJhbG..."`).

**3) Create a claim (with JWT)**  
- Method: `POST`  
- URL: `http://localhost:8080/api/claims/create`  
- Headers:  
  - `Authorization`: `Bearer <paste-your-token>`  
  - `Content-Type`: `application/json`  
- Body (JSON):  
```json
{
  "username": "john",
  "photoUrl": "https://example.com/car-damage.jpg"
}
```  
Alternatively, use query params:  
`POST http://localhost:8080/api/claims/create?username=john&photoUrl=https://example.com/car-damage.jpg`  
with the same `Authorization` header.

**4) List claims (optional)**  
- Method: `GET`  
- URL: `http://localhost:8080/api/claims/all`  
- Header: `Authorization`: `Bearer <your-token>`

---

### Option B: curl (PowerShell)

```powershell
# Register
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"john\",\"password\":\"secret123\"}"

# Login (save the token from the response)
$response = curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"john\",\"password\":\"secret123\"}"
# Copy the "token" value from the JSON output

# Create claim (replace YOUR_TOKEN with the token from login)
curl -X POST "http://localhost:8080/api/claims/create" -H "Authorization: Bearer YOUR_TOKEN" -H "Content-Type: application/json" -d "{\"username\":\"john\",\"photoUrl\":\"https://example.com/car.jpg\"}"
```

---

## Port summary

| Service        | Port | URL base              |
|----------------|------|------------------------|
| Gateway        | 8080 | http://localhost:8080 |
| Auth           | 8081 | (via Gateway only)    |
| Pricing        | 8083 | (via Gateway only)    |
| Claim          | 8085 | (via Gateway only)    |

Always call **http://localhost:8080** for register, login, and claims. The gateway forwards to the right service.

---

## Troubleshooting

- **401 Unauthorized on /api/claims/create**  
  Send the header: `Authorization: Bearer <token>` with the exact token from login.

- **400 Bad Request on create claim**  
  Send either JSON body `{"username":"...","photoUrl":"..."}` or query params `username=...&photoUrl=...`.

- **Connection refused / service not found**  
  Ensure all four services are running and no other app is using ports 8080, 8081, 8083, 8085.

- **Database errors**  
  Ensure PostgreSQL is running and `auth_db`, `claim_db`, and `pricing_db` exist; user `postgres` / password `p1234` (or match your `application.yaml`).

- **Claim creation uses Gemini API**  
  The claim service calls Google’s Gemini API with the key in `claim-service`’s `application.yaml`. If the key is invalid or quota is exceeded, claim creation may fail or return an error in the response.
