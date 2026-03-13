# Frontend Service

Next.js + Tailwind CSS frontend service for the microservices project.

## Features

- Green-themed modern UI/UX
- Reusable UI components (header, footer, buttons, cards, inputs)
- Home page with proper navigation
- Separated customer and admin pages
- Auth service integration functions for:
  - register
  - login
  - validate
  - refresh
  - logout
  - customer profile
  - admin users

## Environment

Create `.env.local` in this folder:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_AUTH_SERVICE_URL=http://localhost:8081
```

## Run

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.
