# Shadow Ledger System - API Testing Guide (Insomnia)

## Prerequisites
Ensure all services are running:
1. **Docker**: Kafka, Zookeeper, PostgreSQL on ports 29092, 2191, 5432
2. **Event Service**: Port 9091
3. **Shadow Ledger Service**: Port 9092
4. **Drift Correction Service**: Port 9093
5. **API Gateway**: Port 9090

---

## 📋 INSOMNIA TEST REQUESTS

### 1️⃣ HEALTH CHECKS (GET Requests)

#### Event Service Health
```
GET http://localhost:9091/actuator/health
```

#### Shadow Ledger Service Health
```
GET http://localhost:9092/actuator/health
```

#### Drift Correction Service Health
```
GET http://localhost:9093/actuator/health
```

#### API Gateway Health
```
GET http://localhost:9090/actuator/health
```

---

### 2️⃣ POST EVENTS (Event Service)

#### POST Event - Credit Transaction
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E1001",
  "accountId": "A10",
  "type": "credit",
  "amount": 500,
  "timestamp": 1735561900000
}
```

#### POST Event - Another Credit
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E1002",
  "accountId": "A10",
  "type": "credit",
  "amount": 300,
  "timestamp": 1735561900000
}
```

#### POST Event - Debit Transaction
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E1003",
  "accountId": "A10",
  "type": "debit",
  "amount": 50,
  "timestamp": 1735562000000
}
```

#### POST Event - Different Account (A11)
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E2001",
  "accountId": "A11",
  "type": "credit",
  "amount": 1500,
  "timestamp": 1735562100000
}
```

#### POST Event - Another for A11
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E2002",
  "accountId": "A11",
  "type": "credit",
  "amount": 50,
  "timestamp": 1735562200000
}
```

**Expected Response:** `202 Accepted`

---

### 3️⃣ GET SHADOW BALANCE (Shadow Ledger Service)

**WAIT 5-10 seconds** after posting events for Kafka to process them!

#### Get Shadow Balance for Account A10
```
GET http://localhost:9092/accounts/A10/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "A10",
  "balance": 750.0,
  "lastEvent": "E1003"
}
```

#### Get Shadow Balance for Account A11
```
GET http://localhost:9092/accounts/A11/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "A11",
  "balance": 1550.0,
  "lastEvent": "E2002"
}
```

#### Get Shadow Balance for Non-Existent Account
```
GET http://localhost:9092/accounts/A99/shadow-balance
```

**Expected Response:**
```json
{
  "accountId": "A99",
  "balance": 0.0,
  "lastEvent": null
}
```

---

### 4️⃣ DRIFT CHECK (Drift Correction Service)

#### Check Drift - No Mismatch
```
POST http://localhost:9093/drift-check
Content-Type: application/json

[
  {
    "accountId": "A10",
    "reportedBalance": 750.0
  },
  {
    "accountId": "A11",
    "reportedBalance": 1550.0
  }
]
```

**Expected Response:** `[]` (empty array - no corrections needed)

#### Check Drift - With Mismatch
```
POST http://localhost:9093/drift-check
Content-Type: application/json

[
  {
    "accountId": "A10",
    "reportedBalance": 900.0
  },
  {
    "accountId": "A11",
    "reportedBalance": 1500.0
  }
]
```

**Expected Response:**
```json
[
  {
    "eventId": "CORR-A10-xxxxx",
    "accountId": "A10",
    "type": "credit",
    "amount": 50.0
  },
  {
    "eventId": "CORR-A11-xxxxx",
    "accountId": "A11",
    "type": "debit",
    "amount": 50.0
  }
]
```

---

### 5️⃣ MANUAL CORRECTION (Drift Correction Service)

#### Manual Correction - Add Credit to Account A10
```
POST http://localhost:9093/correct/A10?type=credit&amount=100
```

**Expected Response:**
```json
{
  "eventId": "MANUAL-A10-xxxxx",
  "accountId": "A10",
  "type": "credit",
  "amount": 100.0
}
```

#### Manual Correction - Add Debit to Account A11
```
POST http://localhost:9093/correct/A11?type=debit&amount=25
```

**Expected Response:**
```json
{
  "eventId": "MANUAL-A11-xxxxx",
  "accountId": "A11",
  "type": "debit",
  "amount": 25.0
}
```

**Note:** After manual corrections, wait 5-10 seconds and check shadow balance again to see the updated balance!

---

### 6️⃣ VIA API GATEWAY (with Security) - BONUS

#### Get JWT Token (if implemented)
```
POST http://localhost:9090/auth/token
Content-Type: application/json

{
  "username": "testuser",
  "role": "USER"
}
```

#### POST Event via Gateway (requires JWT)
```
POST http://localhost:9090/events
Content-Type: application/json
Authorization: Bearer <your-jwt-token>

{
  "eventId": "E3001",
  "accountId": "A12",
  "type": "credit",
  "amount": 1000,
  "timestamp": 1735562300000
}
```

#### Check Drift via Gateway (AUDITOR role)
```
POST http://localhost:9090/drift-check
Content-Type: application/json
Authorization: Bearer <auditor-jwt-token>

[
  {
    "accountId": "A10",
    "reportedBalance": 950.0
  }
]
```

#### Manual Correction via Gateway (ADMIN role)
```
POST http://localhost:9090/correct/A10?type=credit&amount=50
Authorization: Bearer <admin-jwt-token>
```

---

## 🧪 COMPLETE TEST FLOW

### Step-by-Step Testing Sequence:

1. **Check all health endpoints** (should return `{"status":"UP"}`)

2. **Post 5 events** to Event Service (E1001-E1003 for A10, E2001-E2002 for A11)

3. **Wait 10 seconds** for Kafka to process

4. **Get shadow balance** for A10 (should be 750.0)

5. **Get shadow balance** for A11 (should be 1550.0)

6. **Run drift check** with mismatched balances

7. **Verify corrections** were published to Kafka

9. **Wait 10 seconds** for corrections to be processed

9. **Get shadow balance** again to see updated balances

10. **Post manual correction** for testing

11. **Wait 10 seconds**

12. **Get shadow balance** to verify manual correction applied

---

## 📊 EXPECTED FINAL BALANCES (after all test events)

- **Account A10**: 750.0 (500 + 300 - 50)
- **Account A11**: 1550.0 (1500 + 50)

---

## ❌ ERROR SCENARIOS TO TEST

### Duplicate Event ID
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E1001",
  "accountId": "A10",
  "type": "credit",
  "amount": 500,
  "timestamp": 1735561900000
}
```
**Expected:** Error (event ID already exists)

### Invalid Event Type
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E9999",
  "accountId": "A10",
  "type": "invalid",
  "amount": 500,
  "timestamp": 1735561900000
}
```
**Expected:** Validation error

### Negative Amount
```
POST http://localhost:9091/events
Content-Type: application/json

{
  "eventId": "E9999",
  "accountId": "A10",
  "type": "credit",
  "amount": -100,
  "timestamp": 1735561900000
}
```
**Expected:** Validation error (amount must be > 0)

---

## 🔍 MONITORING & DEBUGGING

### Check Kafka Topics
```bash
# If you have Kafka UI running on port 19091
http://localhost:19091
```

### View Database
```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d postgres

# Check ledger events
SELECT * FROM ledger_events ORDER BY timestamp, event_id;

# Check shadow balance calculation
SELECT 
  account_id,
  SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END) as balance,
  COUNT(*) as event_count
FROM ledger_events
GROUP BY account_id;
```

---

## 💡 TIPS

1. **Order matters**: Always post events before checking shadow balance
2. **Wait for Kafka**: Give 5-10 seconds between posting and querying
3. **Check logs**: If responses are unexpected, check service logs
4. **Event IDs must be unique**: Each eventId can only be used once
5. **Timestamps should increase**: Use increasing timestamps for proper ordering

---

## ✅ SUCCESS INDICATORS

- ✅ All health endpoints return `{"status":"UP"}`
- ✅ POST /events returns `202 Accepted`
- ✅ Shadow balance matches expected calculation
- ✅ Drift check detects mismatches correctly
- ✅ Manual corrections are applied and reflected in balance
- ✅ Duplicate event IDs are rejected
- ✅ Invalid data is rejected with appropriate errors


