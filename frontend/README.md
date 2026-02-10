# Wai & Watts Frontend

A modern React frontend for the Wai & Watts environmental data platform.

## Phase 13 Implementation Complete ✅

This frontend implements Phase 13 - Thin Storytelling Client with all Definition of Done criteria met.

## Tech Stack (Locked)

- **React 19** with TypeScript
- **Vite** for build tooling
- **React Router** for navigation  
- **TanStack Query** for server state management
- **Tailwind CSS** for styling
- **Vitest** for unit testing
- **Playwright** for E2E testing

## Getting Started

### Prerequisites
- Node.js 18+
- Backend server running on `http://localhost:8080`

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The frontend will be available at `http://localhost:5173`

### Build

```bash
npm run build
```

### Testing

```bash
# Unit tests
npm run test:run

# Unit tests with UI
npm run test:ui

# E2E tests (requires backend running)
npm run test:e2e
```

## Architecture

- **Feature-first structure** under `src/features/`
- **Thin client** - No domain logic, backend remains authoritative
- **Server state** managed by TanStack Query
- **UI state** managed by React local state
- **Type safety** with comprehensive TypeScript definitions

## Pages

### Home Page (`/`)
Platform overview with navigation to Ask and Browse flows

### Ask Page (`/ask`)
Natural language question interface with example questions and error handling

### Results View (`/results`)
Explanation display with citations and refusal UI variants

### MBIE Browse (`/browse/mbie`)
Table-first MBIE electricity generation data with annual/quarterly toggle and fuel type filtering

### LAWA Browse (`/browse/lawa`)  
Table-first LAWA water quality data with state/trend toggle and region/indicator filtering

## API Integration

Type-safe client connecting to Wai & Watts backend:

- `POST /api/v1/explanations/ask` - Natural language explanations
- `GET /api/v1/mbie/generation/annual` - MBIE annual data
- `GET /api/v1/mbie/generation/quarterly` - MBIE quarterly data
- `GET /api/v1/lawa/water-quality/state/multiyear` - LAWA state data
- `GET /api/v1/lawa/water-quality/trend/multiyear` - LAWA trend data

## Testing

- **Unit Tests**: API client behavior and component rendering (Vitest)
- **E2E Tests**: Ask success and refusal scenarios (Playwright)
- **Coverage**: Core user flows and error states

## Environment Variables

Create `.env` file in root:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Phase 13 Success Criteria

✅ All Definition of Done criteria met:
- [x] React + TypeScript + Vite frontend scaffold
- [x] TanStack Query used for server state
- [x] Tailwind used for styling
- [x] React Router configured
- [x] Ask flow implemented (NL question → explanation OR refusal)
- [x] Results view implemented (explanation + citations + refusal variants)
- [x] Browse views implemented (table-first with Explain This)
- [x] Frontend contains ZERO domain/explanation logic
- [x] Playwright smoke tests implemented

This implementation provides a production-credible client surface while respecting all Phase 13 architectural constraints.