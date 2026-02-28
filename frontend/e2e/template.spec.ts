import { test, expect } from '@playwright/test'
import { assertHomeLoaded } from './support'

test.describe('Basic Smoke Test', () => {
  test('should load home page', async ({ page }) => {
    await assertHomeLoaded(page)
    await expect(page.locator('body')).toBeVisible()
    const askLinks = page.locator('a[href="/ask"]')
    await expect(askLinks.first()).toBeVisible()
    expect(await askLinks.count()).toBeGreaterThanOrEqual(1)
  })
})
