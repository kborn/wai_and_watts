import { test, expect } from '@playwright/test';

test.describe('Ask Refusal Flow', () => {
  test('should load app, enter unsupported request, submit, and see refusal UI', async ({ page }) => {
    // Start at the home page
    await page.goto('/');
    
    // Navigate to the Ask page
    await page.click('text=Ask');
    
    // Enter an unsupported question (forecasting is not supported)
    const unsupportedQuestion = 'Predict electricity generation for 2025';
    await page.fill('#question', unsupportedQuestion);
    
    // Submit the form
    await page.click('button:has-text("Ask Question")');
    
    // Should navigate to results page
    await expect(page).toHaveURL('/results');
    
    // Should show the question
    await expect(page.getByText('Your Question:')).toBeVisible();
    await expect(page.locator('.bg-gray-50 p')).toContainText(unsupportedQuestion);
    
    // Should show refusal UI (distinct from success)
    await expect(page.locator('.border-red-400')).toBeVisible();
    await expect(page.getByText('Question Not Supported')).toBeVisible();
    await expect(page.getByText(/Category:/)).toBeVisible();
  });
});
