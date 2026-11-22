# Cashflow frontend

React 18 + TypeScript + Vite client for the Cash Flow Management platform.

## Local setup

1. Install Node.js 20.19+ and run `npm ci`.
2. Copy `.env.example` to `.env.local` and set the Microsoft Entra SPA client ID, the API scope, and the API gateway URL. Register the Vite origin as an SPA redirect URI in Entra. MSAL uses the OAuth 2.0 authorization-code flow with PKCE.
3. Run `npm run dev` from this directory.

The API client refuses to send requests without a signed-in account and obtains an access token silently for the configured API scope. Do not put client secrets in Vite environment variables; `VITE_*` values are exposed in the browser bundle.

## OpenAPI clients

`contracts/lancamentos-api/openapi.yaml` and `contracts/consolidado-api/openapi.yaml` are the API sources of truth. Regenerate the typed `fetch` clients with `npm run generate:api`; generated files are written to `src/generated` and should not be edited manually. API operations are called through these generated clients and feature query hooks.

## Checks

- `npm run build` — TypeScript check and production build.
- `npm test` — Vitest and React Testing Library.
- `npm run test:coverage` — tests with the configured 80% minimum across statements, branches, functions, and lines. Only generated SDK code and test files are excluded; the MSAL authentication service and application bootstrap are included.
