# Payment Service Reference Guide

This document is a complete reference for the payment-service, including normal checkout flow, Stripe integration, service-to-service communication, data model, API contracts, configuration, testing, and deployment guidance.

## 1. Service Overview

The payment-service is responsible for:

- Recording payment transactions.
- Updating order state to PAID via order-service.
- Decrementing stock in catalog-service after successful checkout.
- Providing payment query endpoints for frontend and admin views.
- Supporting Stripe PaymentIntent-based card payments with webhook confirmation.

### Core capabilities

- Standard checkout endpoint for CARD, CASH, ONLINE flows.
- Stripe create-intent endpoint for secure card collection on frontend.
- Stripe webhook endpoint for asynchronous payment state updates.
- Payment retrieval by payment id, order id, and user id.

## 2. Tech Stack and Dependencies

- Java 17
- Spring Boot 4.0.3
- Spring Web
- Spring Data JPA
- Spring Security (gateway-trust model)
- PostgreSQL
- Stripe Java SDK (24.3.0)
- Gson (2.10.1) for webhook JSON parsing
- SpringDoc OpenAPI
- Actuator

Build file:

- pom.xml

## 3. High-Level Architecture

```text
Frontend (Next.js)
  |
  | /payments/* through API Gateway
  v
payment-service
  |
  |-- PaymentController (standard checkout/query APIs)
  |-- StripeController (create-intent/webhook/status)
  |-- PaymentServiceImpl (checkout business logic)
  |-- StripeService (PaymentIntent + webhook processing)
  |-- PaymentRepository (JPA persistence)
  |
  |---> order-service (PATCH /orders/{id}/status?status=PAID)
  |---> catalog-service (POST /catalog/items/{itemId}/decrement-stock)
  |
  v
PostgreSQL (payments table)
```

## 4. End-to-End Business Flows

### 4.1 Standard checkout flow (non-Stripe)

```text
Frontend -> POST /payments/checkout
  -> PaymentServiceImpl.checkout()
  -> Save Payment(status=COMPLETED, isSuccessCheckout=true)
  -> Call order-service to set status PAID
  -> Call catalog-service decrement-stock
  -> Return PaymentResponse
```

### 4.2 Stripe card flow

```text
User clicks "Pay with Stripe" on checkout page
  |
  | 1) Frontend creates order in order-service
  | 2) Frontend POST /payments/stripe/create-intent
  v
StripeService.createPaymentIntent()
  -> Create Stripe PaymentIntent
  -> Save local Payment(status=PENDING, reference=pi_xxx)
  -> Return clientSecret
  |
  v
Frontend StripeCheckout modal
  -> CardElement collects card details
  -> stripe.confirmCardPayment(clientSecret)
  |
  v
Stripe sends webhook to /payments/stripe/webhook
  -> Signature verification
  -> Update payment status by reference (pi_xxx)
  |
  v
Frontend success callback
  -> Calls /payments/checkout per item with paymentMethod=STRIPE
  -> Triggers order PAID + stock decrement
  -> Redirect to payment success page
```

## 5. API Reference

Base path: /payments

### 5.1 Standard payment endpoints

#### POST /payments/checkout

Process checkout and persist payment.

Request body (PaymentRequest):

```json
{
  "itemId": "ITEM-0001",
  "orderId": "order-001",
  "userId": "user-001",
  "quantity": 2,
  "amount": 25.0,
  "paymentMethod": "CARD"
}
```

Response body (PaymentResponse):

```json
{
  "id": "pay-001",
  "itemId": "ITEM-0001",
  "orderId": "order-001",
  "userId": "user-001",
  "quantity": 2,
  "amount": 25.0,
  "paymentMethod": "CARD",
  "status": "COMPLETED",
  "isSuccessCheckout": true,
  "reference": "PAY-ABCD1234",
  "createdAt": "2026-03-22T10:20:30",
  "updatedAt": "2026-03-22T10:20:30"
}
```

#### GET /payments/orders

Get all payments ordered by createdAt descending.

#### GET /payments/{id}

Get payment by payment id.

#### GET /payments/order/{orderId}

Get payment by order id.

#### GET /payments/user/{userId}

Get all payments for a user.

### 5.2 Stripe endpoints

#### POST /payments/stripe/create-intent

Create Stripe PaymentIntent and local pending record.

Request body (StripeCheckoutRequest):

```json
{
  "orderId": "ORD-20260322-001",
  "userId": "user123",
  "itemId": "ITEM-456",
  "quantity": 2,
  "amount": 250000,
  "currency": "usd",
  "description": "2x Chicken Kottu"
}
```

Response body (StripeCheckoutResponse):

```json
{
  "paymentIntentId": "pi_3RabcXYZ",
  "clientSecret": "pi_3RabcXYZ_secret_...",
  "amount": 250000,
  "currency": "usd",
  "status": "requires_payment_method",
  "orderId": "ORD-20260322-001",
  "paymentRecordId": "PAY-001"
}
```

#### POST /payments/stripe/webhook

Receives Stripe events and updates local payment state.

Required header:

- Stripe-Signature

Success response:

```text
OK
```

#### GET /payments/stripe/status/{paymentIntentId}

Fetch live PaymentIntent state from Stripe.

## 6. Stripe Webhook Behavior

Signature verification:

- Webhook.constructEvent(payload, sigHeader, webhookSecret)

Handled events:

- payment_intent.succeeded -> COMPLETED, isSuccessCheckout=true
- charge.succeeded -> COMPLETED, isSuccessCheckout=true
- charge.failed -> FAILED, isSuccessCheckout=false
- payment_intent.payment_failed -> FAILED, isSuccessCheckout=false
- payment_intent.canceled -> CANCELLED, isSuccessCheckout=false

Lookup strategy:

- PaymentRepository.findByReference(String reference)
- reference stores Stripe PaymentIntent id (pi_xxx)

## 7. Data Model

Entity: Payment

Main fields:

- id: UUID primary key
- itemId: business item id from catalog-service
- orderId: order-service order id
- userId: user identifier
- quantity: quantity purchased
- amount: BigDecimal amount
- paymentMethod: CARD, CASH, ONLINE, STRIPE
- status: PENDING, COMPLETED, FAILED, CANCELLED
- isSuccessCheckout: boolean
- reference: internal payment ref or Stripe pi_xxx
- createdAt, updatedAt

Lifecycle defaults:

- onCreate sets default status PENDING, paymentMethod CARD, quantity 1

## 8. Inter-Service Communication

Catalog integration:

- Client: CatalogServiceClient
- Endpoint called: POST /catalog/items/{itemId}/decrement-stock?quantity=n

Order integration:

- Client: OrderServiceClient
- Endpoint called: PATCH /orders/{id}/status?status=PAID

Behavior in checkout:

- Payment saved first
- Order status update attempted
- Stock decrement attempted
- Client failures are logged; checkout still returns saved payment

## 9. Security Model

- Service uses permit-all authorization internally.
- JWT validation is expected at API Gateway layer.
- CSRF disabled and stateless session policy enabled.

Implication for report:

- This is a trusted internal microservice pattern behind gateway authentication.

## 10. Error Handling Strategy

Global exception handler maps:

- RuntimeException -> 404 Not Found
- DataIntegrityViolationException -> 400 Bad Request
- IllegalArgumentException -> 400 Bad Request
- Any other Exception -> 500 Internal Server Error

Stripe-specific controller behavior:

- Stripe SDK errors in create-intent and status -> 502 style responses
- Invalid webhook signature -> 400 Bad Request

## 11. Configuration and Environment Variables

Main application settings:

- server.port=8084
- server.servlet.context-path=/payments
- spring.jpa.hibernate.ddl-auto=create (change to update for persistent environments)

Database variables:

- DATABASE_URL
- DATABASE_USER
- DATABASE_PASSWORD

Service URL variables:

- CATALOG_SERVICE_URL
- ORDER_SERVICE_URL

Stripe variables:

- STRIPE_SECRET_KEY
- STRIPE_WEBHOOK_SECRET

JWT variable:

- JWT_SECRET

## 12. Local Development and Run Guide

### Option A: Run payment-service directly

```bash
cd payment-service
mvn clean package -DskipTests
mvn spring-boot:run
```

### Option B: Run in Docker Compose

```bash
docker-compose up --build
```

Swagger URL:

- http://localhost:8084/payments/swagger-ui.html

Health endpoint:

- http://localhost:8084/payments/actuator/health

## 13. Stripe Local Testing with Stripe CLI

```bash
stripe login
stripe listen --forward-to localhost:8084/payments/stripe/webhook
stripe trigger payment_intent.succeeded
stripe trigger payment_intent.payment_failed
```

Use generated whsec value as STRIPE_WEBHOOK_SECRET.

Test cards:

- 4242 4242 4242 4242 (success)
- 4000 0000 0000 0002 (declined)
- 4000 0000 0000 9995 (insufficient funds)
- 4000 0025 0000 3155 (3DS)

## 14. Cloud Deployment Notes

1. Deploy service with HTTPS endpoint.
2. Configure Stripe webhook endpoint:
   - https://<your-domain>/payments/stripe/webhook
3. Subscribe webhook events listed in section 6.
4. Set STRIPE_WEBHOOK_SECRET in cloud environment.
5. Prefer externalized secrets (no hardcoded keys in properties for production).
6. Set ddl-auto to update or validate in persistent environments.

## 15. Testing and Validation

Current unit tests include:

- PaymentServiceImplTest with checkout, get-by-id, get-by-order-id, get-by-user coverage.

Run tests:

```bash
cd payment-service
mvn test
```

Recommended additional tests for report quality:

- StripeService unit tests for webhook event mappings.
- Integration tests for create-intent and webhook signature validation.
- Failure-path tests when order-service or catalog-service are unavailable.

## 16. Report-Ready Key Points

You can use these points directly in your project report:

- The payment-service demonstrates both synchronous and asynchronous payment processing patterns.
- Synchronous pattern: standard checkout updates payment, order status, and inventory in one flow.
- Asynchronous pattern: Stripe webhook updates payment status after external gateway confirmation.
- The service uses reference-based reconciliation (Stripe pi_xxx) to map gateway events to local records.
- The microservice is gateway-trusted and stateless, aligning with common cloud-native API gateway architecture.
- The design separates domain checkout logic (PaymentServiceImpl) from gateway logic (StripeService).

## 17. Quick File Map (for Viva and Report)

- controller/PaymentController.java: standard payment APIs
- controller/StripeController.java: Stripe APIs
- service/PaymentServiceImpl.java: checkout business logic
- service/StripeService.java: Stripe intent and webhook logic
- repository/PaymentRepository.java: persistence queries
- entity/Payment.java: table model
- client/CatalogServiceClient.java: stock update integration
- client/OrderServiceClient.java: order status integration
- config/SecurityConfig.java: service security policy
- config/FeignClientConfig.java: RestTemplate bean
- exception/GlobalExceptionHandler.java: error response mapping
