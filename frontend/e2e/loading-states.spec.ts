import { test, expect } from '@playwright/test'

test.describe('Loading States Validation', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('shows loading state during API calls', async ({ page }) => {
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
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

    const loadingText = page.getByText('Loading regions...')
    await expect(loadingText).toBeVisible({ timeout: 5000 })

    await page.waitForTimeout(3000)

    await expect(loadingText).not.toBeVisible()
    await expect(page.locator('select').first()).toBeVisible()
  })

  test('shows loading for fuel types', async ({ page }) => {
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

    await expect(page.getByText('Loading data from backend...')).toBeVisible({
      timeout: 5000,
    })

    await page.waitForTimeout(3000)

    const fuelCheckboxes = fuelSection.locator('input[type="checkbox"]')
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 5000 })
  })

  test('handles API error gracefully', async ({ page }) => {
    await page.route('**/api/v1/mbie/generation/annual/fuel-types', route => {
      return route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' }),
      })
    })

    await page.goto('/browse/mbie')

    await expect(
      page.getByText('Loading data from backend...')
    ).not.toBeVisible({ timeout: 10000 })

    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()

    await expect(page.locator('body')).toBeVisible()
  })
})
