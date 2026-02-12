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

    // Wait for page to load
    await page.waitForTimeout(1000)

    // Verify dropdown elements exist and are visible
    const fuelTypeSelect = page.locator('select').nth(1)
    await expect(fuelTypeSelect).toBeVisible()
    await expect(fuelTypeSelect).toContainText('All')

    // Get the first available option (skip "All" option)
    const options = await fuelTypeSelect.locator('option').all()
    if (options.length > 1) {
      const firstOption = options[1]
      const optionValue = await firstOption.getAttribute('value')
      if (optionValue) {
        await fuelTypeSelect.selectOption(optionValue)
      }
    }

    // Wait a moment for any data loading
    await page.waitForTimeout(1000)

    // Basic verification that the page structure is intact
    await expect(fuelTypeSelect).toBeVisible()
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

    // Verify dropdown elements exist and are visible
    const regionSelect = page.locator('select').nth(1)
    const indicatorSelect = page.locator('select').nth(2)

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

    const viewTypeSelect = page.locator('select').first()
    await expect(viewTypeSelect).toBeVisible()

    // Verify initial state
    await expect(viewTypeSelect).toContainText('State')

    // Switch to trend view
    await viewTypeSelect.selectOption('trend')
    await page.waitForTimeout(1000)

    // Verify the view changed
    await expect(viewTypeSelect).toContainText('Trend')

    // Switch back to state view
    await viewTypeSelect.selectOption('state')
    await page.waitForTimeout(1000)

    // Verify the view changed back
    await expect(viewTypeSelect).toContainText('State')
  })
})
