import { test, expect } from '@playwright/test'

test.describe('End-to-End Dynamic Filter Integration', () => {
  test.beforeEach(async ({ page }) => {
    // Accept any dialogs
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: complete flow from home to filter interaction', async ({
    page,
  }) => {
    // Start at home page
    await page.goto('/')
    await expect(
      page.getByText('Wai & Watts: Environmental Data Platform')
    ).toBeVisible()

    // Navigate to MBIE browse page
    await page.click('a[href="/browse/mbie"]')
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    // Wait for loading or error to appear first
    const loadingOrError = page.getByText(
      /Loading data from backend...|Failed to load data/
    )
    await expect(loadingOrError).toBeVisible({ timeout: 15000 })

    // Wait for loading to disappear (gives us final state)
    await expect(
      page.getByText('Loading data from backend...')
    ).not.toBeVisible({ timeout: 15000 })

    // Small buffer for render
    await page.waitForTimeout(500)

    // Verify Fuel Types container exists
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()

    // Check if there was an error
    const hasError = await page
      .getByText('Failed to load data from backend.')
      .isVisible()
      .catch(() => false)

    // If no error, interact with checkboxes; if error, just verify container is visible
    if (!hasError) {
      const firstFuel = fuelContainer.locator('input[type="checkbox"]').first()
      await expect(firstFuel).toBeVisible()
      await firstFuel.check({ force: true })
      await page.waitForTimeout(1000)
      await expect(firstFuel).toBeVisible()
    }
  })

  test('LAWA: complete flow from home to filter interaction', async ({
    page,
  }) => {
    // Start at home page
    await page.goto('/')
    await expect(
      page.getByText('Wai & Watts: Environmental Data Platform')
    ).toBeVisible()

    // Navigate to LAWA browse page
    await page.click('a[href="/browse/lawa"]')
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    // Wait for page to load
    await page.waitForTimeout(1000)

    // Verify dropdown elements exist and are visible (now at index 0 and 1 since View Type is buttons)
    const regionSelect = page.locator('select').nth(0)
    const indicatorSelect = page.locator('select').nth(1)

    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()
    await expect(regionSelect).toContainText('All')
    await expect(indicatorSelect).toContainText('All')

    // Try to select first available options if they exist
    const regionOptions = await regionSelect.locator('option').all()
    if (regionOptions.length > 1) {
      const firstRegionOption = regionOptions[1]
      const regionValue = await firstRegionOption.getAttribute('value')
      if (regionValue) {
        await regionSelect.selectOption(regionValue)
      }
    }

    const indicatorOptions = await indicatorSelect.locator('option').all()
    if (indicatorOptions.length > 1) {
      const firstIndicatorOption = indicatorOptions[1]
      const indicatorValue = await firstIndicatorOption.getAttribute('value')
      if (indicatorValue) {
        await indicatorSelect.selectOption(indicatorValue)
      }
    }

    // Wait a moment for any data loading
    await page.waitForTimeout(1000)

    // Basic verification that the page structure is intact
    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()
  })

  test('switching between LAWA view types works correctly', async ({
    page,
  }) => {
    await page.goto('/browse/lawa')

    // Wait for page to load
    await page.waitForTimeout(1000)

    // The View Type is now a button group (not a select)
    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })

    await expect(stateButton).toBeVisible()
    await expect(trendButton).toBeVisible()

    // Verify initial state - State button should be selected (primary color)
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    // Click Trend button
    await trendButton.click()
    await page.waitForTimeout(500)

    // Verify Trend is now selected
    await expect(trendButton).toHaveClass(/bg-primary-600/)

    // Click State button
    await stateButton.click()
    await page.waitForTimeout(500)

    // Verify State is now selected again
    await expect(stateButton).toHaveClass(/bg-primary-600/)
  })
})
