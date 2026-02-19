import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from './client'
import type { AskRequest, AskResult } from '../types'

// Explanation hooks
export const useAskQuestion = () => {
  return useMutation<AskResult, Error, AskRequest>({
    mutationFn: (request: AskRequest) => apiClient.askQuestion(request),
  })
}

export const useCapabilities = () => {
  return useQuery({
    queryKey: ['capabilities'],
    queryFn: () => apiClient.getCapabilities(),
  })
}

// MBIE hooks
export const useMbieGenerationAnnual = (params?: {
  fromYear?: number
  toYear?: number
  fuelType?: string
}) => {
  return useQuery({
    queryKey: ['mbie-generation-annual', params],
    queryFn: () => apiClient.getMbieGenerationAnnual(params),
  })
}

export const useMbieGenerationQuarterly = (params?: {
  fromYear?: number
  toYear?: number
  fuelType?: string
}) => {
  return useQuery({
    queryKey: ['mbie-generation-quarterly', params],
    queryFn: () => apiClient.getMbieGenerationQuarterly(params),
  })
}

// LAWA hooks
export const useLawaStateMultiYear = (params?: {
  region?: string
  indicator?: string
}) => {
  return useQuery({
    queryKey: ['lawa-state-multiyear', params],
    queryFn: () => apiClient.getLawaStateMultiYear(params),
  })
}

export const useLawaTrendMultiYear = (params?: {
  region?: string
  indicator?: string
  trend?: string
}) => {
  return useQuery({
    queryKey: ['lawa-trend-multiyear', params],
    queryFn: () => apiClient.getLawaTrendMultiYear(params),
  })
}

// Fuel type hooks
export const useMbieGenerationAnnualFuelTypes = () => {
  return useQuery({
    queryKey: ['mbie-generation-annual-fuel-types'],
    queryFn: () => apiClient.getMbieGenerationAnnualFuelTypes(),
  })
}

export const useMbieGenerationQuarterlyFuelTypes = () => {
  return useQuery({
    queryKey: ['mbie-generation-quarterly-fuel-types'],
    queryFn: () => apiClient.getMbieGenerationQuarterlyFuelTypes(),
  })
}

// LAWA filter hooks
export const useLawaStateRegions = () => {
  return useQuery({
    queryKey: ['lawa-state-regions'],
    queryFn: () => apiClient.getLawaStateRegions(),
  })
}

export const useLawaStateIndicators = () => {
  return useQuery({
    queryKey: ['lawa-state-indicators'],
    queryFn: () => apiClient.getLawaStateIndicators(),
  })
}

export const useLawaTrendRegions = () => {
  return useQuery({
    queryKey: ['lawa-trend-regions'],
    queryFn: () => apiClient.getLawaTrendRegions(),
  })
}

export const useLawaTrendIndicators = () => {
  return useQuery({
    queryKey: ['lawa-trend-indicators'],
    queryFn: () => apiClient.getLawaTrendIndicators(),
  })
}

export const useRegionContext = (params?: {
  region?: string
  indicator?: string
  trendWindow?: number
}) => {
  return useQuery({
    queryKey: ['region-context', params],
    queryFn: () => apiClient.getRegionContext(params),
    enabled: !!params?.region,
  })
}
