# Frontend Service

Next.js + Tailwind CSS frontend service for the microservices project.

## Features

- Green-themed modern UI/UX
- Reusable UI components (header, footer, buttons, cards, inputs, food cards)
- Full food ordering flow for customers:
  - menu discovery
  - category filtering
  - cart management
  - order creation
  - payment processing
  - order history view
- Admin panel for:
  - user status management
  - menu availability toggle
  - order status updates
- Auth service integration functions:
  - register
  - login
  - validate
  - refresh
  - logout
  - profile
  - admin user operations
- Gateway integration for food services:
  - `/catalog/*`
  - `/orders/*`
  - `/payments/*`

## Environment

Create `.env.local` in this folder:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

For Azure deployments, the frontend now resolves the gateway URL at runtime through `/api/runtime-config`, so the same container image can be reused across different Azure accounts and deployments.

## Run

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

## Container Deployment

The repository includes a `Dockerfile` for the frontend so it can be deployed alongside the backend services in Azure Container Apps.

At runtime, set:

```env
NEXT_PUBLIC_API_BASE_URL=https://<gateway-url>
PORT=3000
NODE_ENV=production
```
