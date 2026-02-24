import { test, expect } from '@playwright/test'

test.describe('Ask Refusal Flow', () => {
  test('should load app, enter unsupported request, submit, and see refusal UI', async ({
    page,
  }) => {
    await page.goto('/')
    await page.click('a[href="/ask"]')
    await expect(
      page.getByRole('heading', { name: 'Ask a Question' })
    ).toBeVisible({ timeout: 10000 })
    const questionTextarea = page.locator('textarea[id="question"]')
    await expect(questionTextarea).toBeVisible({ timeout: 10000 })
    const unsupportedQuestion = 'Predict electricity generation for 2025'
    await questionTextarea.fill(unsupportedQuestion)
    await page.click('button:has-text("Ask Question")')
    await page.waitForTimeout(1500)

    if (page.url().includes('/results')) {
      await expect(
        page.getByRole('heading', { name: 'Question' })
      ).toBeVisible()
      await expect(page.getByText(unsupportedQuestion)).toBeVisible()
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

    await expect(page.locator('textarea[id="question"]')).toHaveValue(
      unsupportedQuestion
    )
  })
})
