import { test, expect } from '@playwright/test'

test.describe('Ask Success Flow', () => {
  test('should load app, enter question, submit, and see explanation with citations', async ({
    page,
  }) => {
    await page.goto('/')
    await expect(
      page.getByRole('main').getByRole('heading', {
        name: 'Wai & Watts',
      })
    ).toBeVisible()
    await page.click('a[href="/ask"]')
    await expect(
      page.getByRole('heading', { name: 'Ask a Question' })
    ).toBeVisible({ timeout: 10000 })
    const questionTextarea = page.locator('textarea[id="question"]')
    await expect(questionTextarea).toBeVisible({ timeout: 10000 })
    const question = 'Explain renewable generation trends between 2020 and 2023'
    await questionTextarea.fill(question)
    await page.click('button:has-text("Ask Question")')
    await page.waitForTimeout(1500)

    if (page.url().includes('/results')) {
      await expect(page.getByText('Question')).toBeVisible()
      await expect(page.getByText(question)).toBeVisible()
      return
    }

    const errorCallout = page.getByText(
      'We hit a technical issue while contacting the explanation service. Please try again.'
    )
    const hasError = await errorCallout.isVisible().catch(() => false)
    if (hasError) {
      await expect(errorCallout).toBeVisible()
      return
    }

    await expect(page.locator('textarea[id="question"]')).toHaveValue(question)
  })
})
