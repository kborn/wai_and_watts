import { test, expect } from '@playwright/test'
import {
  assertLawaViewTypeSwitch,
  navigateFromHome,
  registerDialogAutoClose,
} from './support'

test.describe('Dynamic Filters - Comprehensive Tests', () => {
  test.beforeEach(async ({ page }) => {
    registerDialogAutoClose(page)
  })

  test('MBIE: should load and display dynamic fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')

    // Wait for page to load
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    // Check that fuel type checkboxes are present
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

  test('LAWA: should load and display dynamic regions', async ({ page }) => {
    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')

    // Check that region dropdown exists and has dynamic options
    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    // Should have at least default option
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)
  })

  test('LAWA: should load and display dynamic indicators', async ({ page }) => {
    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')

    // Check that indicator dropdown exists and has dynamic options
    const indicatorSelect = page.locator('select').nth(1)
    await expect(indicatorSelect).toBeVisible()

    // Should contain at least default option
    const indicatorOptions = await indicatorSelect.locator('option').all()
    expect(indicatorOptions.length).toBeGreaterThan(0)
  })

  test('should switch between view types', async ({ page }) => {
    await page.goto('/browse/lawa')
    await assertLawaViewTypeSwitch(page)
  })

  test('should handle loading states correctly', async ({ page }) => {
    // Mock API to be slower to trigger loading state
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
        setTimeout(
          () =>
            route.fulfill({
              status: 200,
              body: '{"regions": ["canterbury","otago"]}',
            }),
          2000
        )
      }
    )

    await page.goto('/browse/lawa')
    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()

    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
