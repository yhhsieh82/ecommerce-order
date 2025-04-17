# E-Commerce Order Service

A microservice responsible for handling order creation and inventory stock reservation before payment.

## Overview

This service is part of a microservices-based e-commerce platform. It handles the pre-payment flow of the order process, including:

- Creating new orders
- Reserving inventory stock
- Managing order state transitions
- Handling both retryable and non-retryable errors
- Automatic retry of failed stock reservations
- Monitoring dashboard for order status

## Tech Stack

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- H2 Database (for development)
- Resilience4j for retry and circuit breaking
- WebClient for HTTP communication

## Running the Service

### Prerequisites

- Java 17 or higher
- Maven

### Running Locally

```bash
mvn clean install
mvn spring-boot:run
```

The service will start on port 8080 by default.

## API Endpoints

### Order Management

#### Create Order

```
POST /api/orders
```

Request body:
```json
{
  "customerId": "customer123",
  "items": [
    {
      "productId": "product123",
      "quantity": 2
    }
  ],
  "idempotencyKey": "optional-idempotency-key"
}
```

#### Get Order

```
GET /api/orders/{orderId}
```

#### Get Orders by Customer

```
GET /api/orders/customer/{customerId}
```

#### Get Orders by Status

```
GET /api/orders/status/{status}
```

Valid status values: `CREATED`, `PENDING_RESERVING_STOCK`, `PENDING_PAYMENT`, `INVALID`

#### Process Order

```
POST /api/orders/{orderId}/process
```

### Monitoring Dashboard

#### Order Summary

```
GET /api/dashboard/order-summary
```

Returns count of orders by status.

#### Pending Reservations

```
GET /api/dashboard/pending-reservations
```

Returns all orders in `PENDING_RESERVING_STOCK` status for monitoring.

#### Invalid Orders

```
GET /api/dashboard/invalid-orders
```

Returns all orders in `INVALID` status for troubleshooting.

## Order Flow

1. Order is created with status `CREATED`
2. Processing begins with status `PENDING_RESERVING_STOCK`
3. Inventory service is called to reserve stock
4. On success, status becomes `PENDING_PAYMENT`
5. On non-retryable failure, status becomes `INVALID`
6. Retryable failures keep status as `PENDING_RESERVING_STOCK` for later retry

## Automatic Retry Mechanism

The service includes an automatic scheduler that retries orders in the `PENDING_RESERVING_STOCK` state:

- Retries occur at configurable intervals
- Maximum retry attempts configurable
- Automatic timeout after a specified period
- Retry backoff to prevent overwhelming the inventory service

## Error Handling

The service uses Resilience4j for robust error handling with:
- Retry mechanism with exponential backoff
- Circuit breaker to prevent cascading failures
- Fallback mechanisms for graceful degradation

## Configuration

Key configuration properties (in `application.yml`):

### External Service Configuration
- `external-service.inventory.url`: URL of the inventory service
- `external-service.inventory.timeout`: Timeout for inventory service calls

### Resilience4j Configuration  
- `resilience4j.retry`: Retry configuration for external service calls
- `resilience4j.circuitbreaker`: Circuit breaker configuration

### Scheduler Configuration
- `order-service.scheduler.retry-rate-ms`: How often to run the retry scheduler
- `order-service.scheduler.max-retry-minutes`: Maximum time to retry an order
- `order-service.scheduler.max-attempts`: Maximum number of retry attempts
- `order-service.scheduler.retry-delay-seconds`: Minimum time between retry attempts

## Development Notes

This service is designed to be part of a microservice ecosystem. For local development and testing, it uses an H2 in-memory database. In a production environment, you would configure it to use a persistent database and appropriate service discovery.

The Inventory Service is expected to have an endpoint at `/reserve` that accepts stock reservation requests and follows the defined contract.
