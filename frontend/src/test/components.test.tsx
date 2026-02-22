import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, MemoryRouter } from 'react-router-dom'
import '@testing-library/jest-dom'
import AskPage from '../features/ask/AskPage'
import ResultsPage from '../features/results/ResultsPage'

// Mock API for component testing
vi.mock('../api/hooks', () => ({
  useAskQuestion: vi.fn(() => ({
    mutateAsync: vi.fn().mockResolvedValue({
      isRefusal: false,
      refusal: null,
      parsedRequest: null,
      selectedDatasetSource: null,
      datasetSelection: null,
      explanation: 'Test explanation',
      citations: [],
    }),
    isPending: false,
  })),
  useCapabilities: vi.fn(() => ({
    data: {
      supportedQuestionTypes: {},
      unsupportedQuestionTypes: {},
      supportedDatasetSources: {},
      requiredFilters: {},
      filterStructure: {},
    },
    isLoading: false,
  })),
}))

describe('AskPage Component', () => {
  it('renders ask form with all elements', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>
    )

    expect(
      screen.getByRole('heading', { name: 'Ask About Environmental Data' })
    ).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', { name: 'Your Question' })
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Ask Question' })
    ).toBeInTheDocument()
  })

  it('displays form with placeholder text', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>
    )

    const textarea = screen.getByRole('textbox', { name: 'Your Question' })
    expect(textarea).toHaveAttribute(
      'placeholder',
      'e.g., Explain renewable generation trends between 2020 and 2023'
    )
    expect(
      screen.getByRole('button', { name: 'Ask Question' })
    ).toBeInTheDocument()
  })

  it('has proper form labels and placeholders', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>
    )

    const textarea = screen.getByRole('textbox', { name: 'Your Question' })
    expect(textarea).toHaveAttribute(
      'placeholder',
      'e.g., Explain renewable generation trends between 2020 and 2023'
    )
    expect(textarea).toHaveAttribute('id', 'question')
  })
})

describe('ResultsPage Component', () => {
  it('shows dataset selection when provided', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/results',
              state: {
                question: 'Explain renewable generation trends',
                explanation: {
                  isRefusal: false,
                  refusal: null,
                  parsedRequest: null,
                  selectedDatasetSource: 'mbie.generation.annual',
                  datasetSelection: {
                    strategy: 'LLM_CANDIDATES',
                    reason:
                      'Selected from LLM candidates after verifying filters.',
                  },
                  explanation: 'Test explanation',
                  citations: [],
                },
              },
            },
          ]}
        >
          <ResultsPage />
        </MemoryRouter>
      </QueryClientProvider>
    )

    expect(
      screen.getByText('Using dataset: mbie.generation.annual')
    ).toBeInTheDocument()
    expect(
      screen.getByText('Selected from LLM candidates after verifying filters.')
    ).toBeInTheDocument()
  })

  it('shows refusal callout when refusal response received', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/results',
              state: {
                question: 'Why did hydro fall?',
                explanation: {
                  isRefusal: true,
                  refusal: {
                    code: 'UNSUPPORTED_INTENT',
                    message: 'Wai & Watts explains what published data shows.',
                  },
                  parsedRequest: null,
                  selectedDatasetSource: null,
                  datasetSelection: null,
                  explanation: '',
                  citations: [],
                },
              },
            },
          ]}
        >
          <ResultsPage />
        </MemoryRouter>
      </QueryClientProvider>
    )

    expect(
      screen.getByText('Wai & Watts explains what published data shows.')
    ).toBeInTheDocument()
    expect(screen.getByText('Unsupported Question')).toBeInTheDocument()
  })

  it('shows capability refusal title for capability refusal codes', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/results',
              state: {
                question: 'Compute largest increase',
                explanation: {
                  isRefusal: true,
                  refusal: {
                    code: 'UNSUPPORTED_CAPABILITY',
                    message: 'This capability is not available.',
                  },
                  parsedRequest: null,
                  selectedDatasetSource: null,
                  datasetSelection: null,
                  explanation: '',
                  citations: [],
                },
              },
            },
          ]}
        >
          <ResultsPage />
        </MemoryRouter>
      </QueryClientProvider>
    )

    expect(screen.getByText('Capability Not Available')).toBeInTheDocument()
  })

  it('shows internal error title for internal refusal codes', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/results',
              state: {
                question: 'Any question',
                explanation: {
                  isRefusal: true,
                  refusal: {
                    code: 'INTERNAL_ERROR',
                    message: 'An unexpected error occurred.',
                  },
                  parsedRequest: null,
                  selectedDatasetSource: null,
                  datasetSelection: null,
                  explanation: '',
                  citations: [],
                },
              },
            },
          ]}
        >
          <ResultsPage />
        </MemoryRouter>
      </QueryClientProvider>
    )

    expect(screen.getByText('Internal Processing Error')).toBeInTheDocument()
  })
})
