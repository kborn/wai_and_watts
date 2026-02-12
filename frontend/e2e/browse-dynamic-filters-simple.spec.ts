import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Simple Tests', () => {
  test('MBIE: should navigate and load fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')

    // Check page title
    await expect(page).toHaveTitle(/Wai & Watts/)

    // Check for fuel type dropdown
    const fuelSelect = page.locator('select').filter({ hasText: 'Fuel Type' })
    await expect(fuelSelect).toBeVisible()

    // Check options exist
    const options = await fuelSelect.locator('option').all()
    expect(options.length).toBeGreaterThan(1)
  })

  test('LAWA: should navigate and load regions', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Check page title
    await expect(page).toHaveTitle(/Wai & Watts/)

    // Check for region dropdown
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toBeVisible()

    // Check options exist
    const options = await regionSelect.locator('option').all()
    expect(options.length).toBeGreaterThan(1)
  })
})
