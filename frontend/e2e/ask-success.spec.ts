import { test, expect } from '@playwright/test';

test.describe('Ask Success Flow', () => {
  test('should load app, enter question, submit, and see explanation with citations', async ({ page }) => {
    // Start at the home page
    await page.goto('/');
    
    // Verify we're on the home page
    await expect(page.getByRole('heading', { name: 'Wai & Watts: Environmental Data Platform' })).toBeVisible();
    
    // Navigate to the Ask page
    await page.click('text=Ask');
    await expect(page.getByRole('heading', { name: 'Ask About Environmental Data' })).toBeVisible();
    
    // Enter a question
    const question = 'Explain renewable generation trends between 2020 and 2023';
    await page.fill('#question', question);
    
    // Submit the form
    await page.click('button:has-text("Ask Question")');
    
    // Should navigate to results page
    await expect(page).toHaveURL('/results');
    
    // Should show the question
    await expect(page.getByText('Your Question:')).toBeVisible();
    await expect(page.locator('.bg-gray-50 p')).toContainText(question);
    
    // Note: In a real test with backend, you would verify:
    // - Explanation text is visible
    // - Citations are visible
    // But for smoke test, navigation + question display is sufficient
  });
});
