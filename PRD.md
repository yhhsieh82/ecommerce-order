# 📝 PRD: Placing Order – Pre-Payment Flow

## **Feature Name**
Placing Order – Pre-Payment Flow

## **Goal**
Allow users to place an order and reserve inventory stock **before payment**, ensuring **data consistency** and **robust error handling**.

## **Scope**
Covers the order creation process **up to payment**. Payment logic is **out of scope**.

---

## 🧭 User Flow

1. User submits an order with product and quantity.
2. Order Service creates a new order with status `PENDING_RESERVING_STOCK`.
3. Order Service calls Inventory Service to reserve stock (synchronous HTTP call).
4. On **success**:
    - Update order status to `PENDING_PAYMENT`
    - Redirect user to payment page
5. On **failure**:
    - Retry if the error is retryable
    - Mark order as `INVALID` if non-retryable

---

## 🧩 System Components

| Component           | Role                                                     |
|---------------------|----------------------------------------------------------|
| **Order Service**    | Orchestrates order logic, manages order state           |
| **Inventory Service**| Reserves stock, validates product/quantity availability |
| **Order DB**         | Stores order state                                       |
| **Redis (optional)** | Used by Inventory for locking/reservation cache         |

---

## 🔁 Order Status Lifecycle (Pre-Payment)

| Status                   | Description                                |
|--------------------------|--------------------------------------------|
| `CREATED`                | Initial state                              |
| `PENDING_RESERVING_STOCK`| Reserving stock from inventory             |
| `PENDING_PAYMENT`        | Stock reserved, ready for payment          |
| `INVALID`                | Failed business validation or unrecoverable error |

---

## ⚠️ Failure Scenarios and Handling

### ✅ Retryable Errors (Transient)

| Case               | Description                                   | Handling Strategy                      |
|--------------------|-----------------------------------------------|----------------------------------------|
| Network Timeout    | No response from Inventory                    | Retry with backoff                     |
| 5xx Server Errors  | Inventory internal error (502, 503, etc.)     | Retry with circuit breaker             |
| Redis Timeout      | Redis temporarily unavailable                 | Retry request                          |
| Lock Contention    | Concurrent stock reservation                  | Retry with delay                       |
| Deployment Lag     | Inventory service restarting                  | Retry, optional user-facing fallback   |

> ✅ Order remains in `PENDING_RESERVING_STOCK` during retries.

---

### ❌ Non-Retryable Errors (Business Logic or Client-Side)

| Case               | Description                                   | Handling Strategy                      |
|--------------------|-----------------------------------------------|----------------------------------------|
| Out of Stock       | No stock left                                 | Mark order as `INVALID`, notify user   |
| Invalid Product ID | Nonexistent product                           | Mark order as `INVALID`, log issue     |
| Bad Quantity       | Zero or negative quantity                     | Mark order as `INVALID`, validate input|
| Duplicate Order    | Repeated order submission                     | Use idempotency key to prevent repeat  |

> ❌ Order transitions to `INVALID`. No retries attempted.

---

### ⚠️ Ambiguous / Edge Case Failures

| Case                                            | Description                                          | Handling Strategy                                         |
|-------------------------------------------------|------------------------------------------------------|-----------------------------------------------------------|
| Inventory succeeded, but Order crashed          | Order status not updated after reservation           | Use transactional outbox or message queue                |
| Inventory succeeded but client timed out        | Risk of double reservation                           | Use idempotent token (e.g., order ID) in Inventory call  |
| Order DB update failed after inventory success  | Reserved stock but no status change in Order DB      | Retry logic with rollback alert; consider saga pattern    |

---

## 🛠 Non-Functional Requirements

- **Idempotency:** Inventory reservation must be idempotent per order ID.
- **Timeouts:** All external service calls must have a reasonable timeout.
- **Retry Policy:** Exponential backoff with retry limit.
- **Reservation Expiry:** Reserved stock should auto-expire after N minutes (e.g., 15).
- **Monitoring:** All failures should be logged with alerts for operational awareness.

---

## ⛔ Out of Scope

- Payment flow and integration
- Post-payment order handling (`PAID`, `SHIPPED`, etc.)
- Order cancellation or refund mechanisms

---

## 🚀 Suggested Enhancements

| Feature                       | Benefit                                       |
|-------------------------------|-----------------------------------------------|
| RetryTemplate / Resilience4j | Retry & circuit breaking                      |
| Distributed Tracing           | Easier debugging of cross-service calls       |
| Dead Letter Queue (DLQ)       | Handle failures after retry exhaustion        |
| Transactional Outbox Pattern  | Ensure cross-service consistency              |
| Monitoring Dashboard          | Track status transitions and system health    |

---
