import { expect, type Locator, type Page } from '@playwright/test'

export function registerDialogAutoClose(page: Page) {
  page.on('dialog', () => {
    page.close()
  })
}

export async function assertHomeLoaded(page: Page) {
  await page.goto('/')
  await expect(
    page.getByRole('main').getByText('Environmental Data Platform')
  ).toBeVisible()
}

export async function navigateFromHome(
  page: Page,
  href: string,
  heading: string
) {
  await assertHomeLoaded(page)
  await page.click(`a[href="${href}"]`)
  await expect(page.getByText(heading)).toBeVisible()
}

export async function waitForBackendBrowseState(page: Page) {
  const loadingOrError = page.getByText(
    /Loading data from backend...|Failed to load data from backend\.?|Failed to load data|Showing/
  )
  await expect(loadingOrError).toBeVisible({ timeout: 15000 })
  await expect(page.getByText('Loading data from backend...')).not.toBeVisible({
    timeout: 15000,
  })
  await page.waitForTimeout(500)
}

export function fuelTypesContainer(page: Page) {
  return page.locator('xpath=//div[label[normalize-space()="Fuel Types"]]')
}

export async function hasBackendLoadError(page: Page) {
  return page
    .getByText('Failed to load data from backend.')
    .isVisible()
    .catch(() => false)
}

export function lawaSelects(page: Page) {
  return {
    regionSelect: page.locator('select').nth(0),
    indicatorSelect: page.locator('select').nth(1),
  }
}

export async function assertLawaSelectsVisible(page: Page) {
  const { regionSelect, indicatorSelect } = lawaSelects(page)
  await expect(regionSelect).toBeVisible()
  await expect(indicatorSelect).toBeVisible()
  await expect(regionSelect).toContainText('All')
  await expect(indicatorSelect).toContainText('All')
  return { regionSelect, indicatorSelect }
}

export async function selectFirstNonDefaultOption(select: Locator) {
  const options = await select.locator('option').all()
  if (options.length <= 1) {
    return
  }
  const value = await options[1].getAttribute('value')
  if (value) {
    await select.selectOption(value)
  }
}

export async function assertLawaViewTypeSwitch(page: Page) {
  const stateButton = page.getByRole('button', { name: 'State' })
  const trendButton = page.getByRole('button', { name: 'Trend' })

  await expect(stateButton).toBeVisible()
  await expect(trendButton).toBeVisible()
  await expect(stateButton).toHaveClass(/bg-primary-600/)

  await trendButton.click()
  await page.waitForTimeout(500)
  await expect(trendButton).toHaveClass(/bg-primary-600/)

  return { stateButton, trendButton }
}
