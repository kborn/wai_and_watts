import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Focused Tests', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: should navigate and load dynamic fuel types', async ({
    page,
  }) => {
    await page.goto('/browse/mbie')
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible({
      timeout: 10000,
    })

    const fuelTypeContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    const fuelCheckboxes = fuelTypeContainer.locator('input[type="checkbox"]')
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 10000 })
    expect(await fuelCheckboxes.count()).toBeGreaterThan(0)
  })

  test('LAWA: should navigate and load dynamic regions', async ({ page }) => {
    await page.goto('/browse/lawa')
    await expect(page.getByText('LAWA Water Quality')).toBeVisible({
      timeout: 10000,
    })

    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)
  })

  test('should show loading states during data fetch', async ({ page }) => {
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
    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
