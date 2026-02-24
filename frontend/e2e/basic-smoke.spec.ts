import { test, expect } from '@playwright/test'

test.describe('Basic Smoke Test', () => {
  test('should load home page', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('body')).toBeVisible()
    await expect(
      page.getByRole('main').getByRole('heading', { name: 'Wai & Watts' })
    ).toBeVisible()
    const askLinks = page.locator('a[href="/ask"]')
    await expect(askLinks.first()).toBeVisible()
    expect(await askLinks.count()).toBeGreaterThanOrEqual(1)
  })
})
