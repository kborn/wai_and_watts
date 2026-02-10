import type {
  AskRequest,
  MbieGenerationAnnualRecord,
  MbieGenerationQuarterlyRecord,
  LawaStateMultiYearRecord,
  LawaTrendMultiYearRecord,
  Explanation,
  CapabilitiesResponse,
} from '../types'
import { logger } from '../utils/logger'
import { addDiagnostic } from '../utils/diagnostics'
import {
  API_BASE_URL,
  generateRequestId,
  classifyError,
  getErrorResponseSnippet,
} from '../utils/apiUtils'

class ApiClient {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const requestId = generateRequestId()
    const url = `${API_BASE_URL}${endpoint}`
    const method = options.method || 'GET'
    const startTime = Date.now()

    logger.info(`API Request: ${requestId} ${method} ${endpoint}`)

    try {
      const response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          'X-Request-Id': requestId,
          ...options.headers,
        },
        ...options,
      })

      const duration = Date.now() - startTime

      if (!response.ok) {
        const errorInfo = classifyError(response)
        const snippet = await getErrorResponseSnippet(response)

        addDiagnostic({
          requestId,
          endpoint,
          method,
          status: response.status,
          errorType: errorInfo.type,
          errorMessage: errorInfo.message,
          duration,
        })

        logger.error(
          `API Error: ${requestId} ${endpoint} - ${errorInfo.type} - ${errorInfo.message} - snippet: ${snippet}`
        )

        throw new Error(`API Error: ${response.status} ${response.statusText}`)
      }

      addDiagnostic({
        requestId,
        endpoint,
        method,
        status: response.status,
        duration,
      })

      logger.info(
        `API Success: ${requestId} ${endpoint} - ${response.status} - ${duration}ms`
      )

      return response.json()
    } catch (error) {
      const duration = Date.now() - startTime
      const errorInfo = classifyError(error as Error)

      addDiagnostic({
        requestId,
        endpoint,
        method,
        errorType: errorInfo.type,
        errorMessage: errorInfo.message,
        duration,
      })

      logger.error(
        `API Failure: ${requestId} ${endpoint} - ${errorInfo.type} - ${duration}ms`,
        error
      )

      throw error
    }
  }

  // Explanation endpoints
  async askQuestion(request: AskRequest): Promise<Explanation> {
    return this.request<Explanation>('/api/v1/explanations/ask', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  }

  async getCapabilities(): Promise<CapabilitiesResponse> {
    return this.request<CapabilitiesResponse>(
      '/api/v1/explanations/capabilities'
    )
  }

  // MBIE endpoints
  async getMbieGenerationAnnual(params?: {
    fromYear?: number
    toYear?: number
    source?: string
    fuelType?: string
  }): Promise<MbieGenerationAnnualRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.fromYear)
      searchParams.append('fromYear', params.fromYear.toString())
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString())
    if (params?.source) searchParams.append('source', params.source)
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType)

    const query = searchParams.toString()
    return this.request<MbieGenerationAnnualRecord[]>(
      `/api/v1/mbie/generation/annual${query ? `?${query}` : ''}`
    )
  }

  async getMbieGenerationQuarterly(params?: {
    fromYear?: number
    toYear?: number
    source?: string
    fuelType?: string
  }): Promise<MbieGenerationQuarterlyRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.fromYear)
      searchParams.append('fromYear', params.fromYear.toString())
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString())
    if (params?.source) searchParams.append('source', params.source)
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType)

    const query = searchParams.toString()
    return this.request<MbieGenerationQuarterlyRecord[]>(
      `/api/v1/mbie/generation/quarterly${query ? `?${query}` : ''}`
    )
  }

  // LAWA endpoints
  async getLawaStateMultiYear(params?: {
    region?: string
    indicator?: string
  }): Promise<LawaStateMultiYearRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.region) searchParams.append('region', params.region)
    if (params?.indicator) searchParams.append('indicator', params.indicator)

    const query = searchParams.toString()
    return this.request<LawaStateMultiYearRecord[]>(
      `/api/v1/lawa/water-quality/state/multiyear${query ? `?${query}` : ''}`
    )
  }

  async getLawaTrendMultiYear(params?: {
    region?: string
    indicator?: string
    trend?: string
  }): Promise<LawaTrendMultiYearRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.region) searchParams.append('region', params.region)
    if (params?.indicator) searchParams.append('indicator', params.indicator)
    if (params?.trend) searchParams.append('trend', params.trend)

    const query = searchParams.toString()
    return this.request<LawaTrendMultiYearRecord[]>(
      `/api/v1/lawa/water-quality/trend/multiyear${query ? `?${query}` : ''}`
    )
  }
}

export const apiClient = new ApiClient()
