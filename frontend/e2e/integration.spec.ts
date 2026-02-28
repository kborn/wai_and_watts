import { test, expect } from '@playwright/test'
import {
  assertLawaSelectsVisible,
  assertLawaViewTypeSwitch,
  fuelTypesContainer,
  hasBackendLoadError,
  navigateFromHome,
  registerDialogAutoClose,
  selectFirstNonDefaultOption,
  waitForBackendBrowseState,
} from './support'

test.describe('End-to-End Dynamic Filter Integration', () => {
  test.beforeEach(async ({ page }) => {
    registerDialogAutoClose(page)
  })

  test('MBIE: complete flow from home to filter interaction', async ({
    page,
  }) => {
    await navigateFromHome(page, '/browse/mbie', 'MBIE Electricity Generation')
    await waitForBackendBrowseState(page)
    const fuelContainer = fuelTypesContainer(page)
    await expect(fuelContainer).toBeVisible()
    const hasError = await hasBackendLoadError(page)
    if (!hasError) {
      const firstFuel = fuelContainer.locator('input[type="checkbox"]').first()
      await expect(firstFuel).toBeVisible()
      await firstFuel.check({ force: true })
      await page.waitForTimeout(1000)
      await expect(firstFuel).toBeVisible()
    }
  })

  test('LAWA: complete flow from home to filter interaction', async ({
    page,
  }) => {
    await navigateFromHome(page, '/browse/lawa', 'LAWA Water Quality')
    await page.waitForTimeout(1000)
    const { regionSelect, indicatorSelect } =
      await assertLawaSelectsVisible(page)
    await selectFirstNonDefaultOption(regionSelect)
    await selectFirstNonDefaultOption(indicatorSelect)
    await page.waitForTimeout(1000)
    await expect(regionSelect).toBeVisible()
    await expect(indicatorSelect).toBeVisible()
  })

  test('switching between LAWA view types works correctly', async ({
    page,
  }) => {
    await page.goto('/browse/lawa')
    await page.waitForTimeout(1000)
    const { stateButton, trendButton } = await assertLawaViewTypeSwitch(page)
    await stateButton.click()
    await page.waitForTimeout(500)
    await expect(stateButton).toHaveClass(/bg-primary-600/)
    await expect(trendButton).toBeVisible()
  })
})
