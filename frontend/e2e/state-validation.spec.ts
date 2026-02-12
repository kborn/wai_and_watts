import { test, expect } from '@playwright/test'

test.describe('Current State Validation', () => {
  test('MBIE page should be accessible', async ({ page }) => {
    console.log('Starting MBIE page test...')

    // Navigate to MBIE page
    await page.goto('/browse/mbie')

    // Wait for any navigation or loading
    await page.waitForTimeout(3000)

    // Check if page loaded at all
    const pageTitle = await page.title()
    console.log('Page title:', pageTitle)

    // Get the entire page content for debugging
    const pageContent = await page.content()
    console.log(
      'Page has MBIE text:',
      pageContent.includes('MBIE Electricity Generation')
    )

    // Simple check - does page have any select elements?
    const selects = await page.locator('select').all()
    console.log('Number of select elements:', selects.length)

    // Basic check - page should load
    expect(pageContent.includes('MBIE Electricity Generation')).toBe(true)
  })
})
