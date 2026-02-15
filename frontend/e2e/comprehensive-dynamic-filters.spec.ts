import { test, expect } from '@playwright/test'

test.describe('Dynamic Filters - Comprehensive Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Accept any dialogs
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: should load and display dynamic fuel types', async ({ page }) => {
    await page.goto('/browse/mbie')

    // Wait for page to load
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    // Check that fuel type dropdown exists and has dynamic options
    const fuelTypeSelect = page
      .locator('select')
      .filter({ hasText: 'Fuel Type' })
    await expect(fuelTypeSelect).toBeVisible()

    // Get all fuel type options
    const fuelOptions = await fuelTypeSelect.locator('option').all()
    expect(fuelOptions.length).toBeGreaterThan(0)

    // Should contain expected fuel types (case-insensitive)
    const fuelTexts = await Promise.all(
      fuelOptions.map(option => option.textContent())
    )
    expect(fuelTexts.some(text => text.toLowerCase().includes('hydro'))).toBe(
      true
    )
    expect(fuelTexts.some(text => text.toLowerCase().includes('wind'))).toBe(
      true
    )
    expect(fuelTexts.some(text => text.toLowerCase().includes('solar'))).toBe(
      true
    )
  })

  test('LAWA: should load and display dynamic regions', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Wait for page to load
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    // Check that region dropdown exists and has dynamic options
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toBeVisible()

    // Should have many more regions than the original 3 hardcoded
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(5) // More than original hardcoded

    // Should contain major regions
    const regionTexts = await Promise.all(
      regionOptions.map(option => option.textContent())
    )
    expect(
      regionTexts.some(text => text.toLowerCase().includes('canterbury'))
    ).toBe(true)
    expect(
      regionTexts.some(text => text.toLowerCase().includes('auckland'))
    ).toBe(true)
  })

  test('LAWA: should load and display dynamic indicators', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Wait for page to load
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    // Check that indicator dropdown exists and has dynamic options
    const indicatorSelect = page
      .locator('select')
      .filter({ hasText: 'Indicator' })
    await expect(indicatorSelect).toBeVisible()

    // Should contain expected indicators
    const indicatorOptions = await indicatorSelect.locator('option').all()
    expect(indicatorOptions.length).toBeGreaterThan(0)

    // Should contain expected indicators
    const indicatorTexts = await Promise.all(
      indicatorOptions.map(option => option.textContent())
    )
    expect(
      indicatorTexts.some(text => text.toLowerCase().includes('e. coli'))
    ).toBe(true)
    expect(
      indicatorTexts.some(text => text.toLowerCase().includes('nitrogen'))
    ).toBe(true)
  })

  test('should switch between view types', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Should default to state view - State button should be selected
    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    // Switch to trend view
    await trendButton.click()
    await page.waitForTimeout(500)
    await expect(trendButton).toHaveClass(/bg-primary-600/)
  })

  test('should handle loading states correctly', async ({ page }) => {
    await page.goto('/browse/lawa')

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

    // Initially region select should be enabled
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toBeEnabled()

    // Wait for loading state to appear
    await expect(page.getByText('Loading regions...')).toBeVisible({
      timeout: 5000,
    })
  })
})
