import type {
  AskRequest,
  AskResult,
  CapabilitiesResponse,
  LawaStateMultiYearRecord,
  LawaTrendMultiYearRecord,
  MbieGenerationAnnualRecord,
  MbieGenerationQuarterlyRecord,
  RegionContextFactPack,
} from '../types'
import { logger } from '../utils/logger'
import { addDiagnostic } from '../utils/diagnostics'
import {
  API_BASE_URL,
  classifyError,
  generateRequestId,
  getErrorResponseSnippet,
} from '../utils/apiUtils'

// Backend response DTO (matches Java AskResult class)
type BackendAskResult = AskResult

// Custom error class for HTTP errors to distinguish from network failures
export class HttpError extends Error {
  public readonly status: number
  public readonly statusText: string
  public readonly errorInfo: { type: string; message: string }
  public readonly snippet?: string

  constructor(
    status: number,
    statusText: string,
    errorInfo: { type: string; message: string },
    snippet?: string
  ) {
    super(`HTTP Error: ${status} ${statusText}`)
    this.name = 'HttpError'
    this.status = status
    this.statusText = statusText
    this.errorInfo = errorInfo
    this.snippet = snippet
  }
}

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

    let response: Response
    let duration: number

    try {
      response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          'Request-Id': requestId,
          ...options.headers,
        },
        ...options,
      })
      duration = Date.now() - startTime
    } catch (error) {
      // Network/fetch errors only
      duration = Date.now() - startTime
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

    // Handle HTTP errors (separate from network errors)
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

      throw new HttpError(
        response.status,
        response.statusText,
        errorInfo,
        snippet
      )
    }

    // Success path - parse JSON
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

    try {
      return await response.json()
    } catch (parseError) {
      logger.error(
        `JSON Parse Error: ${requestId} ${endpoint} - Failed to parse response body`,
        parseError
      )
      throw new Error(`Invalid JSON response from ${endpoint}`)
    }
  }

  // Explanation endpoints
  async askQuestion(request: AskRequest): Promise<AskResult> {
    return await this.request<BackendAskResult>('/api/v1/explanations/ask', {
      method: 'POST',
      body: JSON.stringify(request),
    })
  }

  async getCapabilities(): Promise<CapabilitiesResponse> {
    try {
      return await this.request<CapabilitiesResponse>('/api/v1/capabilities')
    } catch {
      // Backward-compatible fallback for older backend deployments.
      return await this.request<CapabilitiesResponse>(
        '/api/v1/explanations/capabilities'
      )
    }
  }

  // MBIE endpoints
  async getMbieGenerationAnnual(params?: {
    fromYear?: number
    toYear?: number
    fuelType?: string
  }): Promise<MbieGenerationAnnualRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.fromYear)
      searchParams.append('fromYear', params.fromYear.toString())
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString())
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType)

    const query = searchParams.toString()
    return this.request<MbieGenerationAnnualRecord[]>(
      `/api/v1/mbie/generation/annual${query ? `?${query}` : ''}`
    )
  }

  async getMbieGenerationQuarterly(params?: {
    fromYear?: number
    toYear?: number
    fuelType?: string
  }): Promise<MbieGenerationQuarterlyRecord[]> {
    const searchParams = new URLSearchParams()
    if (params?.fromYear)
      searchParams.append('fromYear', params.fromYear.toString())
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString())
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType)

    const query = searchParams.toString()
    return this.request<MbieGenerationQuarterlyRecord[]>(
      `/api/v1/mbie/generation/quarterly${query ? `?${query}` : ''}`
    )
  }

  async getMbieGenerationAnnualFuelTypes(): Promise<string[]> {
    const response = await this.request<{ fuelTypes: string[] }>(
      '/api/v1/mbie/generation/annual/fuel-types'
    )
    return response.fuelTypes
  }

  async getMbieGenerationQuarterlyFuelTypes(): Promise<string[]> {
    const response = await this.request<{ fuelTypes: string[] }>(
      '/api/v1/mbie/generation/quarterly/fuel-types'
    )
    return response.fuelTypes
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

  async getLawaStateRegions(): Promise<string[]> {
    const response = await this.request<{ regions: string[] }>(
      '/api/v1/lawa/water-quality/state/multiyear/regions'
    )
    return response.regions
  }

  async getLawaStateIndicators(): Promise<string[]> {
    const response = await this.request<{ indicators: string[] }>(
      '/api/v1/lawa/water-quality/state/multiyear/indicators'
    )
    return response.indicators
  }

  async getLawaTrendRegions(): Promise<string[]> {
    const response = await this.request<{ regions: string[] }>(
      '/api/v1/lawa/water-quality/trend/multiyear/regions'
    )
    return response.regions
  }

  async getLawaTrendIndicators(): Promise<string[]> {
    const response = await this.request<{ indicators: string[] }>(
      '/api/v1/lawa/water-quality/trend/multiyear/indicators'
    )
    return response.indicators
  }

  async getRegionContext(params?: {
    region?: string
    indicator?: string
    trendWindow?: number
  }): Promise<RegionContextFactPack> {
    const searchParams = new URLSearchParams()
    if (params?.region) searchParams.append('region', params.region)
    if (params?.indicator) searchParams.append('indicator', params.indicator)
    if (params?.trendWindow)
      searchParams.append('trendWindow', params.trendWindow.toString())

    const query = searchParams.toString()
    return this.request<RegionContextFactPack>(
      `/api/v1/region-context${query ? `?${query}` : ''}`
    )
  }
}

export const apiClient = new ApiClient()
