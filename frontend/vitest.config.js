import { defineConfig } from 'vitest/config';
export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
        exclude: ['e2e/**/*'],
    },
});
