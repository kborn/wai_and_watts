// Navigation and UI Types
export type Route = '/' | '/ask' | '/results' | '/browse/mbie' | '/browse/lawa';

export interface NavBarProps {
  currentRoute?: Route;
}

// API Types
export interface ExplanationRequest {
  questionType: string;
  datasetSource: string;
  filters?: {
    fuelType?: string;
    indicator?: string;
    region?: string;
    trend?: string;
    startYear?: number;
    endYear?: number;
  };
}

export interface Explanation {
  explanation?: string;
  citations?: Citation[];
  refusalCategory?: string;
  refusalReason?: string;
}

export interface Citation {
  factPackId?: string;
  dataset?: string;
  field?: string;
  value?: string;
  source?: string;
}

export interface AskRequest {
  question: string;
}

// MBIE Types
export interface MbieGenerationAnnualRecord {
  id: string;
  periodYear: number;
  fuelTypeRaw: string;
  fuelTypeNorm: string;
  generationGwh: number;
}

export interface MbieGenerationQuarterlyRecord {
  id: string;
  periodYear: number;
  periodQuarter: number;
  fuelTypeRaw: string;
  fuelTypeNorm: string;
  generationGwh: number;
}

// LAWA Types
export interface LawaStateMultiYearRecord {
  id: string;
  periodStartYear: number;
  periodEndYear: number;
  region: string;
  site: string;
  indicatorRaw: string;
  indicatorNorm: string;
  stateRaw: string;
  stateNorm: string;
}

export interface LawaTrendMultiYearRecord {
  id: string;
  asOfYear: number;
  periodType: string;
  periodStartYear: number;
  periodEndYear: number;
  region: string;
  site: string;
  indicatorRaw: string;
  indicatorNorm: string;
  trendClassification: string;
  trendScore?: number;
}