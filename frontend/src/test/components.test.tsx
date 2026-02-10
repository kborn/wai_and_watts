import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import AskPage from '../features/ask/AskPage';

// Mock API for component testing
vi.mock('../api/hooks', () => ({
  useAskQuestion: vi.fn(() => ({
    mutateAsync: vi.fn().mockResolvedValue({
      explanation: 'Test explanation',
      citations: [],
    }),
    isPending: false,
  })),
}));

describe('AskPage Component', () => {
  it('renders ask form with all elements', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole('heading', { name: 'Ask About Environmental Data' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Your Question' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ask Question' })).toBeInTheDocument();
  });

  it('displays form with placeholder text', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const textarea = screen.getByRole('textbox', { name: 'Your Question' });
    expect(textarea).toHaveAttribute('placeholder', 'e.g., Explain renewable generation trends between 2020 and 2023');
    expect(screen.getByRole('button', { name: 'Ask Question' })).toBeInTheDocument();
  });

  it('has proper form labels and placeholders', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AskPage />
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const textarea = screen.getByRole('textbox', { name: 'Your Question' });
    expect(textarea).toHaveAttribute('placeholder', 'e.g., Explain renewable generation trends between 2020 and 2023');
    expect(textarea).toHaveAttribute('id', 'question');
  });
});
