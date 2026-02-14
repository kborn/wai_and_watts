// Navigation and UI Types
export type Route = '/' | '/ask' | '/results' | '/browse/mbie' | '/browse/lawa'

export interface NavBarProps {
  currentRoute?: Route
}

// API Types
export interface ExplanationRequest {
  questionType: string
  datasetSource: string
  filters?: {
    fuelType?: string
    indicator?: string
    region?: string
    trend?: string
    startYear?: number
    endYear?: number
  }
}

export interface Explanation {
  explanation?: string
  citations?: Citation[]
  refusalCategory?: string
  refusalReason?: string
}

export interface Citation {
  factPackId?: string
  dataset?: string
  field?: string
  value?: string
  source?: string
}

export interface AskRequest {
  question: string
}

// MBIE Types
export interface MbieGenerationAnnualRecord {
  id: string
  periodYear: number
  fuelType?: string
  fuelTypeRaw: string
  fuelTypeNorm?: string
  generationGwh: number
  releaseId: string
}

export interface MbieGenerationQuarterlyRecord {
  id: string
  periodYear: number
  periodQuarter: number
  fuelType?: string
  fuelTypeRaw: string
  fuelTypeNorm?: string
  generationGwh: number
  releaseId: string
}

// LAWA Types
export interface LawaStateMultiYearRecord {
  id: string
  periodStartYear: number
  periodEndYear: number
  periodType: string
  region: string
  siteName: string
  lawaSiteId: string
  indicatorRaw: string
  indicatorNorm: string
  attributeBand: string
  stateNorm: string
  units: string
  latitude: string
  longitude: string
  median: number | null
  p95: number | null
  recHealthExceed260Pct: number | null
  recHealthExceed540Pct: number | null
}

export interface LawaTrendMultiYearRecord {
  id: string
  periodStartYear: number
  periodEndYear: number
  periodType: string
  region: string
  siteName: string
  lawaSiteId: string
  indicatorRaw: string
  indicatorNorm: string
  trendRaw: string
  trendNorm: string
  trendScore: number | null
  trendPeriodYears: number
  trendDataFrequency: string
  units: string
  latitude: string
  longitude: string
}

// API Capabilities Types
export interface CapabilitiesResponse {
  supportedQuestionTypes: Record<string, string>
  unsupportedQuestionTypes: Record<string, string>
  supportedDatasetSources: Record<string, string>
  requiredFilters: Record<string, string>
  filterStructure: Record<string, string>
}
