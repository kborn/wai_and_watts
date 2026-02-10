import { expect, afterEach } from 'vitest';
import * as matchers from '@testing-library/jest-dom/matchers';

// Extend Vitest's expect
expect.extend(matchers);

afterEach(() => {
  // Cleanup after each test
});