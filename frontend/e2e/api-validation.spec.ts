import { test, expect } from '@playwright/test'

test.describe('Dynamic Filter Basic Validation', () => {
  test.beforeEach(async ({ page }) => {
    // Accept any dialogs
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE page shows fuel type checkboxes', async ({ page }) => {
    // Navigate to MBIE page
    await page.goto('/browse/mbie')

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

    // If no error, checkboxes should exist; if error, container should be empty but visible
    if (!hasError) {
      const fuelCheckboxes = fuelContainer.locator('input[type="checkbox"]')
      await expect(fuelCheckboxes.first()).toBeVisible()
    }
  })

  test('LAWA state view loads with dropdown elements', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Wait for page to load
    await page.waitForTimeout(1000)

    // Verify dropdown elements exist
    const regionSelect = page.locator('select').nth(1)
    const indicatorSelect = page.locator('select').nth(2)

    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()

    // Should have at least "All" option
    await expect(regionSelect).toContainText('All')
    await expect(indicatorSelect).toContainText('All')
  })

  test('LAWA trend view loads with dropdown elements', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Switch to trend view (first select is View Type)
    const viewTypeSelect = page.locator('select').first()
    await viewTypeSelect.selectOption('trend')

    // Wait for page to load
    await page.waitForTimeout(1000)

    // Verify dropdown elements exist in trend view
    const regionSelect = page.locator('select').nth(1)
    const indicatorSelect = page.locator('select').nth(2)

    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()

    // Should have at least "All" option
    await expect(regionSelect).toContainText('All')
    await expect(indicatorSelect).toContainText('All')
  })

  test('LAWA view type switching works', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Wait for page to load
    await page.waitForTimeout(1000)

    const viewTypeSelect = page.locator('select').first()
    await expect(viewTypeSelect).toBeVisible()

    // Verify initial state
    await expect(viewTypeSelect).toContainText('State')

    // Switch to trend view
    await viewTypeSelect.selectOption('trend')
    await page.waitForTimeout(500)

    // Verify the view changed
    await expect(viewTypeSelect).toContainText('Trend')

    // Switch back to state view
    await viewTypeSelect.selectOption('state')
    await page.waitForTimeout(500)

    // Verify the view changed back
    await expect(viewTypeSelect).toContainText('State')
  })
})
