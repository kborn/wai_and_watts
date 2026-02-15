import { test, expect } from '@playwright/test'

test.describe('MBIE Browse Page - Dynamic Filters', () => {
  test('should load dynamic fuel type options', async ({ page }) => {
    // Go to MBIE browse page
    await page.goto('/')
    await page.click('a[href="/browse/mbie"]')

    // Wait for fuel type dropdown to be populated
    await expect(
      page.locator('select').filter({ hasText: 'Fuel Type' })
    ).toBeVisible()

    // Check that all expected fuel types are present
    const fuelTypeSelect = page
      .locator('select')
      .filter({ hasText: 'Fuel Type' })
    await expect(fuelTypeSelect).toContainText([
      'All',
      'Hydro',
      'Wind',
      'Solar',
      'Geothermal',
      'Other',
      'Gas',
      'Coal',
    ])
  })

  test('should switch between annual and quarterly views', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/mbie"]')

    // Should default to annual view
    await expect(
      page.locator('select').filter({ hasText: 'Annual' })
    ).toHaveValue('annual')

    // Switch to quarterly
    const annualSelect = page.locator('select').filter({ hasText: 'Annual' })
    await annualSelect.selectOption('quarterly')
    await expect(annualSelect).toHaveValue('quarterly')
  })
})

test.describe('LAWA Browse Page - Dynamic Filters', () => {
  test('should load dynamic region and indicator options', async ({ page }) => {
    // Go to LAWA browse page
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')

    // Wait for dropdowns to be populated
    await expect(
      page.locator('select').filter({ hasText: 'Region' })
    ).toBeVisible()
    await expect(
      page.locator('select').filter({ hasText: 'Indicator' })
    ).toBeVisible()

    // Check that regions are loaded dynamically (more than hardcoded 3)
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toContainText('All')
    // Should contain many more regions than just hardcoded ones
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(3) // More than original 3 hardcoded

    // Check indicators are loaded
    const indicatorSelect = page
      .locator('select')
      .filter({ hasText: 'Indicator' })
    await expect(indicatorSelect).toContainText([
      'All',
      'E. coli',
      'Nitrogen',
      'Phosphorus',
    ])
  })

  test('should switch between state and trend views', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')

    // Should default to state view - State button should be selected
    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    // Switch to trend
    await trendButton.click()
    await page.waitForTimeout(500)
    await expect(trendButton).toHaveClass(/bg-primary-600/)
  })

  test('should show loading states while fetching filters', async ({
    page,
  }) => {
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')

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

    // Should show loading indicator initially
    const regionSelect = page.locator('select').filter({ hasText: 'Region' })
    await expect(regionSelect).toHaveAttribute('disabled', '')

    // Verify loading text appears
    await expect(page.locator('text=Loading regions...')).toBeVisible()
  })
})
