import type { ExplanationRequest, AskRequest, MbieGenerationAnnualRecord, MbieGenerationQuarterlyRecord, LawaStateMultiYearRecord, LawaTrendMultiYearRecord, Explanation } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Explanation endpoints
  async askQuestion(request: AskRequest): Promise<Explanation> {
    return this.request<Explanation>('/api/v1/explanations/ask', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getCapabilities(): Promise<any> {
    return this.request<any>('/api/v1/explanations/capabilities');
  }

  // MBIE endpoints
  async getMbieGenerationAnnual(params?: {
    fromYear?: number;
    toYear?: number;
    source?: string;
    fuelType?: string;
  }): Promise<MbieGenerationAnnualRecord[]> {
    const searchParams = new URLSearchParams();
    if (params?.fromYear) searchParams.append('fromYear', params.fromYear.toString());
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString());
    if (params?.source) searchParams.append('source', params.source);
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType);
    
    const query = searchParams.toString();
    return this.request<MbieGenerationAnnualRecord[]>(`/api/v1/mbie/generation/annual${query ? `?${query}` : ''}`);
  }

  async getMbieGenerationQuarterly(params?: {
    fromYear?: number;
    toYear?: number;
    source?: string;
    fuelType?: string;
  }): Promise<MbieGenerationQuarterlyRecord[]> {
    const searchParams = new URLSearchParams();
    if (params?.fromYear) searchParams.append('fromYear', params.fromYear.toString());
    if (params?.toYear) searchParams.append('toYear', params.toYear.toString());
    if (params?.source) searchParams.append('source', params.source);
    if (params?.fuelType) searchParams.append('fuelType', params.fuelType);
    
    const query = searchParams.toString();
    return this.request<MbieGenerationQuarterlyRecord[]>(`/api/v1/mbie/generation/quarterly${query ? `?${query}` : ''}`);
  }

  // LAWA endpoints
  async getLawaStateMultiYear(params?: {
    region?: string;
    indicator?: string;
  }): Promise<LawaStateMultiYearRecord[]> {
    const searchParams = new URLSearchParams();
    if (params?.region) searchParams.append('region', params.region);
    if (params?.indicator) searchParams.append('indicator', params.indicator);
    
    const query = searchParams.toString();
    return this.request<LawaStateMultiYearRecord[]>(`/api/v1/lawa/water-quality/state/multiyear${query ? `?${query}` : ''}`);
  }

  async getLawaTrendMultiYear(params?: {
    region?: string;
    indicator?: string;
    trend?: string;
  }): Promise<LawaTrendMultiYearRecord[]> {
    const searchParams = new URLSearchParams();
    if (params?.region) searchParams.append('region', params.region);
    if (params?.indicator) searchParams.append('indicator', params.indicator);
    if (params?.trend) searchParams.append('trend', params.trend);
    
    const query = searchParams.toString();
    return this.request<LawaTrendMultiYearRecord[]>(`/api/v1/lawa/water-quality/trend/multiyear${query ? `?${query}` : ''}`);
  }
}

export const apiClient = new ApiClient();