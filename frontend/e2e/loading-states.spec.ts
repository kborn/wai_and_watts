import { test, expect } from '@playwright/test'

test.describe('Loading States Validation', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('shows loading state during API calls', async ({ page }) => {
    // Mock slow API response BEFORE navigating to trigger loading
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
        // Simulate slow network
        setTimeout(() => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ regions: ['canterbury', 'auckland'] }),
          })
        }, 2000)
      }
    )

    await page.goto('/browse/lawa')

    // Should show loading text
    const loadingText = page.getByText('Loading regions...')
    await expect(loadingText).toBeVisible({ timeout: 5000 })

    // Wait for API call to complete
    await page.waitForTimeout(3000)

    // Loading should disappear and regions should appear
    await expect(loadingText).not.toBeVisible()
    await expect(page.locator('select').nth(1)).toContainText('All')
  })

  test('shows loading for fuel types', async ({ page }) => {
    // Mock slow fuel types API BEFORE navigating
    await page.route('**/api/v1/mbie/generation/annual/fuel-types', route => {
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fuelTypes: ['HYDRO', 'WIND'] }),
        })
      }, 1500)
    })

    await page.goto('/browse/mbie')

    const fuelSection = page.locator('div', {
      has: page.getByText('Fuel Types'),
    })
    const fuelCheckboxes = fuelSection.locator('input[type="checkbox"]')

    // During loading, the page indicates backend loading
    await expect(page.getByText('Loading data from backend...')).toBeVisible({
      timeout: 5000,
    })

    // Wait for completion
    await page.waitForTimeout(3000)

    // Options should appear after the API resolves — ensure mocked labels are present
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 5000 })
    // Verify labels within the Fuel Types container to avoid matching chart text
    await expect(
      fuelSection.locator('label', { hasText: 'HYDRO' })
    ).toBeVisible()
    await expect(
      fuelSection.locator('label', { hasText: 'WIND' })
    ).toBeVisible()
  })

  test('handles API error gracefully', async ({ page }) => {
    // Mock API error BEFORE navigating
    await page.route('**/api/v1/mbie/generation/annual/fuel-types', route => {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' }),
      })
    })
    await page.route(
      '**/api/v1/mbie/generation/quarterly/fuel-types',
      route => {
        return route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Internal server error' }),
        })
      }
    )

    await page.goto('/browse/mbie')

    // Wait for loading to complete (either success or error)
    await expect(
      page.getByText('Loading data from backend...')
    ).not.toBeVisible({
      timeout: 10000,
    })
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    // Keep Fuel Types section visible; it should be empty or unchanged but not crash the page
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()
    const fuelOptionList = fuelContainer.locator('div.flex.flex-wrap.gap-2')
    await expect(fuelOptionList).toBeVisible()
    await expect(fuelOptionList.locator('input[type="checkbox"]')).toHaveCount(
      0
    )
  })
})
