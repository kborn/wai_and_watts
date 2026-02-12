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

    // Check that fuel type dropdown exists
    const fuelTypeSelect = page
      .locator('select')
      .filter({ hasText: 'Fuel Type' })
    await expect(fuelTypeSelect).toBeVisible()

    // Check that we have dynamic options (more than just hardcoded)
    const fuelOptions = await fuelTypeSelect.locator('option').all()
    expect(fuelOptions.length).toBeGreaterThan(5) // Should have many fuel types
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
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toBeVisible()

    // Should have many regions (loaded from API)
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(10) // Should have more than hardcoded 3
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

    const regionSelect = page.locator('select').filter({ hasText: 'Region' })

    // Initially should be enabled
    await expect(regionSelect).toBeEnabled()

    // Should show loading when API is slow
    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
