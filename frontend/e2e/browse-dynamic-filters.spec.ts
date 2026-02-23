import { test, expect } from '@playwright/test'

test.describe('MBIE Browse Page - Dynamic Filters', () => {
  test('should load dynamic fuel type options', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/mbie"]')
    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()
    const fuelCheckboxes = fuelContainer.locator('input[type="checkbox"]')
    await expect(fuelCheckboxes.first()).toBeVisible({ timeout: 10000 })
    expect(await fuelCheckboxes.count()).toBeGreaterThan(0)
  })

  test('should switch between annual and quarterly views', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/mbie"]')

    const annualSelect = page.locator('select').first()
    await expect(annualSelect).toHaveValue('annual')
    await annualSelect.selectOption('quarterly')
    await expect(annualSelect).toHaveValue('quarterly')
  })
})

test.describe('LAWA Browse Page - Dynamic Filters', () => {
  test('should load dynamic region and indicator options', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')
    const regionSelect = page.locator('select').first()
    const indicatorSelect = page.locator('select').nth(1)
    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()
    const regionOptions = await regionSelect.locator('option').all()
    expect(regionOptions.length).toBeGreaterThan(0)
    const indicatorOptions = await indicatorSelect.locator('option').all()
    expect(indicatorOptions.length).toBeGreaterThan(0)
  })

  test('should switch between state and trend views', async ({ page }) => {
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')

    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    await trendButton.click()
    await page.waitForTimeout(500)
    await expect(trendButton).toHaveClass(/bg-primary-600/)
  })

  test('should show loading states while fetching filters', async ({
    page,
  }) => {
    await page.route(
      '**/api/v1/lawa/water-quality/state/multiyear/regions',
      route => {
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
    await page.goto('/')
    await page.click('a[href="/browse/lawa"]')

    const loadingHint = page.getByText('Loading regions...')
    await expect(loadingHint).toBeVisible({ timeout: 5000 })
  })
})
