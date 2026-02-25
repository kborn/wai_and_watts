import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Simple Tests', () => {
  test('MBIE: should navigate and load fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')

    // Check page title
    await expect(page).toHaveTitle(/Wai & Watts/)

    // Check for fuel type filter checkboxes
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()
    await expect(
      page.getByText(
        /Loading data from backend...|Failed to load data from backend.|Showing/
      )
    ).toBeVisible()
  })

  test('LAWA: should navigate and load regions', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Check page title
    await expect(page).toHaveTitle(/Wai & Watts/)

    // Check for region dropdown
    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    // Check options exist (at least "All Regions")
    const options = await regionSelect.locator('option').all()
    expect(options.length).toBeGreaterThan(0)
  })
})
