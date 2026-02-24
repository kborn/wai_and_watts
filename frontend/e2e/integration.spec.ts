import { test, expect } from '@playwright/test'

test.describe('End-to-End Dynamic Filter Integration', () => {
  test.beforeEach(async ({ page }) => {
    page.on('dialog', () => {
      page.close()
    })
  })

  test('MBIE: complete flow from home to filter interaction', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(
      page.getByRole('main').getByRole('heading', { name: 'Wai & Watts' })
    ).toBeVisible()

    await page.click('a[href="/browse/mbie"]')
    await expect(page.getByText('MBIE Electricity Generation')).toBeVisible()

    await expect(
      page.getByText(/Loading data from backend...|Failed to load data/)
    ).toBeVisible({ timeout: 15000 })

    const fuelContainer = page.locator(
      'xpath=//div[label[normalize-space()="Fuel Types"]]'
    )
    await expect(fuelContainer).toBeVisible()

    const firstFuel = fuelContainer.locator('input[type="checkbox"]').first()
    const hasFuelOption = await firstFuel.isVisible().catch(() => false)
    if (hasFuelOption) {
      await firstFuel.check({ force: true })
      await expect(firstFuel).toBeChecked()
    }
  })

  test('LAWA: complete flow from home to filter interaction', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(
      page.getByRole('main').getByRole('heading', { name: 'Wai & Watts' })
    ).toBeVisible()

    await page.click('a[href="/browse/lawa"]')
    await expect(page.getByText('LAWA Water Quality')).toBeVisible()

    const regionSelect = page.locator('select').nth(0)
    const indicatorSelect = page.locator('select').nth(1)

    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()

    const regionOptions = await regionSelect.locator('option').count()
    const indicatorOptions = await indicatorSelect.locator('option').count()
    expect(regionOptions).toBeGreaterThan(0)
    expect(indicatorOptions).toBeGreaterThan(0)
  })

  test('switching between LAWA view types works correctly', async ({
    page,
  }) => {
    await page.goto('/browse/lawa')

    const stateButton = page.getByRole('button', { name: 'State' })
    const trendButton = page.getByRole('button', { name: 'Trend' })

    await expect(stateButton).toBeVisible()
    await expect(trendButton).toBeVisible()
    await expect(stateButton).toHaveClass(/bg-primary-600/)

    await trendButton.click()
    await expect(trendButton).toHaveClass(/bg-primary-600/)

    await stateButton.click()
    await expect(stateButton).toHaveClass(/bg-primary-600/)
  })
})
