import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiClient } from '../api/client'
import type { AskRequest } from '../types'

// Mock fetch for testing
const mockFetch = vi.fn()

describe('API Client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('askQuestion', () => {
    it('should make a POST request to /api/v1/explanations/ask', async () => {
      const mockResponse = {
        explanation: 'Test explanation',
        citations: [
          { dataset: 'test', field: 'test', value: 'test', source: 'test' },
        ],
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValueOnce(mockResponse),
      } as unknown as Response)

      const request: AskRequest = { question: 'Test question' }
      const result = await apiClient.askQuestion(request)

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/explanations/ask',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(request),
        }
      )
      expect(result).toEqual(mockResponse)
    })

    it('should throw error when request fails', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      } as Response)

      const request: AskRequest = { question: 'Test question' }

      await expect(apiClient.askQuestion(request)).rejects.toThrow(
        'API Error: 500 Internal Server Error'
      )
    })
  })
})
