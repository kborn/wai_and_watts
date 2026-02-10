import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from './client'
import type { AskRequest, Explanation } from '../types'

// Explanation hooks
export const useAskQuestion = () => {
  return useMutation<Explanation, Error, AskRequest>({
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
  source?: string
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
  source?: string
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
