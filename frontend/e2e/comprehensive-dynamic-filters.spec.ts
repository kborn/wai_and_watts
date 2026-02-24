import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Comprehensive Tests', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: should load and display dynamic fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    const fuelCheckboxes = fuelContainer.locator('input[type="checkbox"]')
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 10000 })
    expect(await fuelCheckboxes.count()).toBeGreaterThan(0)
  })

  test('LAWA: should load and display dynamic regions', async ({ page }) => {
    await page.goto('/browse/lawa')
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    const regionSelect = page.locator('select').first()
    await expect(regionSelect).toBeVisible()
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)
  })

  test('LAWA: should load and display dynamic indicators', async ({ page }) => {
    await page.goto('/browse/lawa')
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    const indicatorSelect = page.locator('select').nth(1)
    await expect(indicatorSelect).toBeVisible()
    const indicatorOptions = await indicatorSelect.locator('option').all()
    expect(indicatorOptions.length).toBeGreaterThan(0)
  })

  test('should switch between view types', async ({ page }) => {
    await page.goto('/browse/lawa')

    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    await trendButton.click()
    await page.waitForTimeout(500)
    await expect(trendButton).toHaveClass(/bg-primary-600/)
  })

  test('should handle loading states correctly', async ({ page }) => {
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
    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
