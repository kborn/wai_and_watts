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
      // Mock backend DTO response
      const mockBackendResponse = {
        isRefusal: false,
        refusal: null,
        parsedRequest: null,
        selectedDatasetSource: null,
        datasetSelection: null,
        explanation: 'Test explanation',
        citations: [
          {
            id: 'ts:mbie:generation_gwh:HYDRO:1974_2024',
            type: 'TIME_SERIES',
          },
        ],
      }

      // Expected frontend interface after mapping
      const expectedFrontendResponse = mockBackendResponse

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValueOnce(mockBackendResponse),
      } as unknown as Response)

      const request: AskRequest = { question: 'Test question' }
      const result = await apiClient.askQuestion(request)

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/explanations/ask',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Request-Id': expect.any(String),
          }),
          body: JSON.stringify(request),
        })
      )
      expect(result).toEqual(expectedFrontendResponse)
    })

    it('should throw error when request fails', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      } as Response)

      const request: AskRequest = { question: 'Test question' }

      await expect(apiClient.askQuestion(request)).rejects.toThrow(
        expect.objectContaining({
          message: expect.stringContaining('500 Internal Server Error'),
          name: 'HttpError',
        })
      )
    })
  })
})
