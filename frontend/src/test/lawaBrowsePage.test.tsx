import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import LawaBrowsePage from '../features/browse-lawa/LawaBrowsePage'

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="echarts" />,
}))

vi.mock('../features/browse-lawa/RegionContextPanel', () => ({
  default: () => <div data-testid="region-context-panel" />,
}))

vi.mock('../api/hooks', () => ({
  useLawaStateMultiYear: vi.fn(() => ({
    data: [],
    isLoading: false,
    error: null,
  })),
  useLawaTrendMultiYear: vi.fn(() => ({
    data: [],
    isLoading: false,
    error: null,
  })),
  useLawaStateRegions: vi.fn(() => ({
    data: ['canterbury'],
    isLoading: false,
  })),
  useLawaStateIndicators: vi.fn(() => ({
    data: ['E. coli', 'Nitrate'],
    isLoading: false,
  })),
  useLawaTrendRegions: vi.fn(() => ({
    data: ['canterbury'],
    isLoading: false,
  })),
  useLawaTrendIndicators: vi.fn(() => ({
    data: ['Ammoniacal nitrogen', 'Visual clarity'],
    isLoading: false,
  })),
}))

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <LawaBrowsePage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('LawaBrowsePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('preserves indicator selection when toggling between state and trend', () => {
    renderPage()

    const indicatorSelect = screen.getAllByRole('combobox')[1]
    fireEvent.change(indicatorSelect, { target: { value: 'E. coli' } })
    expect(screen.getAllByRole('combobox')[1]).toHaveValue('E. coli')

    fireEvent.click(screen.getByRole('button', { name: 'Trend' }))
    expect(screen.getAllByRole('combobox')[1]).toHaveValue('E. coli')
  })
})
