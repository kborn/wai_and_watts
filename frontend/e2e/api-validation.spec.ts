import { test, expect } from '@playwright/test'
import {
  assertLawaSelectsVisible,
  assertLawaViewTypeSwitch,
  fuelTypesContainer,
  hasBackendLoadError,
  registerDialogAutoClose,
  waitForBackendBrowseState,
} from './support'

test.describe('Dynamic Filter Basic Validation', () => {
  test.beforeEach(async ({ page }) => {
    registerDialogAutoClose(page)
  })

  test('MBIE page shows fuel type checkboxes', async ({ page }) => {
    await page.goto('/browse/mbie')
    await waitForBackendBrowseState(page)
    const fuelContainer = fuelTypesContainer(page)
    await expect(fuelContainer).toBeVisible()
    const hasError = await hasBackendLoadError(page)
    if (!hasError) {
      const fuelCheckboxes = fuelContainer.locator('input[type="checkbox"]')
      await expect(fuelCheckboxes.first()).toBeVisible()
    }
  })

  test('LAWA state view loads with dropdown elements', async ({ page }) => {
    await page.goto('/browse/lawa')
    await page.waitForTimeout(1000)
    await assertLawaSelectsVisible(page)
  })

  test('LAWA trend view loads with dropdown elements', async ({ page }) => {
    await page.goto('/browse/lawa')
    const trendButton = page.getByRole('button', { name: 'Trend' })
    await trendButton.click()
    await page.waitForTimeout(1000)
    await assertLawaSelectsVisible(page)
  })

  test('LAWA view type switching works', async ({ page }) => {
    await page.goto('/browse/lawa')
    await page.waitForTimeout(1000)
    const { stateButton, trendButton } = await assertLawaViewTypeSwitch(page)
    await stateButton.click()
    await page.waitForTimeout(500)
    await expect(stateButton).toHaveClass(/bg-primary-600/)
    await expect(trendButton).toBeVisible()
  })
})
