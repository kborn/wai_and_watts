# E2E Tests for Dynamic Filter Implementation

This directory contains comprehensive end-to-end tests for the dynamic filter feature implementation.

## Test Coverage

### Core Functionality Tests

- `basic-smoke.spec.ts` - Basic navigation and page loading
- `state-validation.spec.ts` - Verifies pages are accessible and render correctly

### API Integration Tests

- `api-validation.spec.ts` - Validates that dynamic filter endpoints work correctly
- `integration.spec.ts` - Complete end-to-end flows from home to filtered data

### UI State Tests

- `loading-states.spec.ts` - Validates loading states during API calls

## Test Features

### Dynamic Filter APIs Tested

- ✅ MBIE annual fuel types: `/api/v1/mbie/generation/annual/fuel-types`
- ✅ LAWA state regions: `/api/v1/lawa/water-quality/state/multiyear/regions`
- ✅ LAWA state indicators: `/api/v1/lawa/water-quality/state/multiyear/indicators`
- ✅ LAWA trend regions: `/api/v1/lawa/water-quality/trend/multiyear/regions`
- ✅ LAWA trend indicators: `/api/v1/lawa/water-quality/trend/multiyear/indicators`

### Frontend Integration Tested

- ✅ Dynamic dropdown population
- ✅ Filter selection and application
- ✅ View type switching (MBIE annual/quarterly, LAWA state/trend)
- ✅ Loading states during API calls
- ✅ Error handling and graceful degradation

### Running Tests

```bash
npm run test:e2e
```

### CI Integration

Tests run automatically on:

- Pull requests
- Push to main branch
- Results uploaded as GitHub Actions artifacts

All tests use API mocking to ensure consistent, reliable validation of the dynamic filter functionality.
