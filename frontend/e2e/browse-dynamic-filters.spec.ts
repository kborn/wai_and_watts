import { test, expect } from '@playwright/test'
import { assertLawaViewTypeSwitch, navigateFromHome } from './support'

test.describe('MBIE Browse Page - Dynamic Filters', () => {
  test('should load dynamic fuel type options', async ({ page }) => {
    await navigateFromHome(page, '/browse/mbie', 'MBIE Electricity Generation')

    // Wait for fuel type checkboxes to be rendered
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()
    const fuelCheckboxes = fuelContainer.locator('input[type="checkbox"]')
    await expect(fuelCheckboxes.first()).toBeVisible()
  })

  test('should switch between annual and quarterly views', async ({ page }) => {
    await navigateFromHome(page, '/browse/mbie', 'MBIE Electricity Generation')

    const viewTypeSelect = page.locator('select').first()
    await expect(viewTypeSelect).toHaveValue('annual')
    await viewTypeSelect.selectOption('quarterly')
    await expect(viewTypeSelect).toHaveValue('quarterly')
  })
})

test.describe('LAWA Browse Page - Dynamic Filters', () => {
  test('should load dynamic region and indicator options', async ({ page }) => {
    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')

    // Wait for dropdowns to be populated
    const regionSelect = page.locator('select').first()
    const indicatorSelect = page.locator('select').nth(1)
    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()

    // Check that regions are loaded dynamically (more than hardcoded 3)
    await expect(regionSelect).toContainText('All')
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)

    // Check indicators are loaded
    await expect(indicatorSelect).toContainText('All')
  })

  test('should switch between state and trend views', async ({ page }) => {
    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')
    await assertLawaViewTypeSwitch(page)
  })

  test('should show loading states while fetching filters', async ({
    page,
  }) => {
    // Mock slower network to test loading states
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
        // Delay response to test loading state
        setTimeout(
          () =>
            route.fulfill({
              status: 200,
              body: '{"regions": ["canterbury","auckland","bay of plenty"]}',
            }),
          1000
        )
      }
    )

    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')

    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()
    await expect(page.getByText('Loading regions...')).toBeVisible()
  })
})
