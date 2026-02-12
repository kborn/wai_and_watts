import { test, expect } from '@playwright/test'

test.describe('Loading States Validation', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('shows loading state during API calls', async ({ page }) => {
    await page.goto('/browse/lawa')

    // Mock slow API response to trigger loading
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
    await page.goto('/browse/mbie')

    // Mock slow fuel types API
    await page.route('**/api/v1/mbie/generation/annual/fuel-types', route => {
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ fuelTypes: ['HYDRO', 'WIND'] }),
        })
      }, 1500)
    })

    // Should show loading
    await expect(page.getByText('Loading fuel types...')).toBeVisible({
      timeout: 5000,
    })

    // Wait for completion
    await page.waitForTimeout(3000)

    // Loading should disappear, options should appear
    await expect(page.getByText('Loading fuel types...')).not.toBeVisible()
    await expect(page.locator('select').nth(1)).toContainText('All')
  })

  test('handles API error gracefully', async ({ page }) => {
    await page.goto('/browse/mbie')

    // Mock API error
    await page.route('**/api/v1/mbie/generation/annual/fuel-types', route => {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' }),
      })
    })

    // Should handle error gracefully
    await page.waitForTimeout(2000)

    // Should show error state or fallback
    const fuelSelect = page.locator('select').nth(1)
    await expect(fuelSelect).toBeVisible() // Should still show selector
  })
})
