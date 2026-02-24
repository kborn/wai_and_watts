import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Focused Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Accept any dialogs
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: should navigate and load dynamic fuel types', async ({
    page,
  }) => {
    // Navigate directly to MBIE page
    await page.goto('/browse/mbie')

    // Wait for page to load
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible({
      timeout: 10000,
    })

    // Wait a moment for data to load
    await page.waitForTimeout(2000)

    // Check that fuel type checkboxes exist
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()
    const fuelOptions = await fuelContainer
      .locator('input[type="checkbox"]')
      .all()
    expect(fuelOptions.length).toBeGreaterThan(0)
  })

  test('LAWA: should navigate and load dynamic regions', async ({ page }) => {
    // Navigate directly to LAWA page
    await page.goto('/browse/lawa')

    // Wait for page to load
    await expect(page.getByText('LAWA Water Quality')).toBeVisible({
      timeout: 10000,
    })

    // Wait a moment for data to load
    await page.waitForTimeout(2000)

    // Check that region dropdown exists
    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    // Should have at least default option
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)
  })

  test('should show loading states during data fetch', async ({ page }) => {
    // Mock API to be slower
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
        setTimeout(
          () =>
            route.fulfill({
              status: 200,
              body: '{"regions": ["canterbury","auckland","bay of plenty"]}',
            }),
          2000
        )
      }
    )

    await page.goto('/browse/lawa')

    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    // Should show loading when API is slow
    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
