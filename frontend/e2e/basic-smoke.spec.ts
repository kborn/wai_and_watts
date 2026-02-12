import { test, expect } from '@playwright/test'

test.describe('Basic Smoke Test', () => {
  test('should load home page', async ({ page }) => {
    // Start at the home page
    await page.goto('/')

    // Check if page loads at all
    await expect(page.locator('body')).toBeVisible()

    // Check for main heading
    const heading = page.getByText('Wai & Watts: Environmental Data Platform')
    await expect(heading).toBeVisible()

    // Try to find any link to /ask (should be at least 1 - NavBar + HomePage)
    const askLinks = page.locator('a[href="/ask"]')
    await expect(askLinks.first()).toBeVisible()
    expect(await askLinks.count()).toBeGreaterThanOrEqual(1)

    console.log('Page title:', await page.title())
    console.log('Ask links found:', await askLinks.count())
  })
})
