import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Simple Tests', () => {
  test('MBIE: should navigate and load fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')
    await expect(page).toHaveTitle(/Wai & Watts/)
    await expect(page.getByText('Fuel Types')).toBeVisible()
    const fuelCheckboxes = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]//input[@type="checkbox"]'
    )
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 10000 })
    expect(await fuelCheckboxes.count()).toBeGreaterThan(0)
  })

  test('LAWA: should navigate and load regions', async ({ page }) => {
    await page.goto('/browse/lawa')
    await expect(page).toHaveTitle(/Wai & Watts/)
    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()
    const options = await regionSelect.locator('option').all()
    expect(options.length).toBeGreaterThan(0)
  })
})
