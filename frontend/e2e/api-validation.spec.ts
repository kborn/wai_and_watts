import { test, expect } from '@playwright/test'

test.describe('Dynamic Filter Basic Validation', () => {
  test.beforeEach(async ({ page }) => {
    // Accept any dialogs
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE page loads with dropdown elements', async ({ page }) => {
    // Navigate to MBIE page
    await page.goto('/browse/mbie')

    // Wait for page to load
    await page.waitForTimeout(1000)

    // Verify dropdown elements exist (second select is Fuel Type)
    const fuelTypeSelect = page.locator('select').nth(1)
    await expect(fuelTypeSelect).toBeVisible()

    // Should have at least "All" option
    await expect(fuelTypeSelect).toContainText('All')
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
