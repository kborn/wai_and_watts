import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/Layout';
import HomePage from './features/home/HomePage';
import AskPage from './features/ask/AskPage';
import ResultsPage from './features/results/ResultsPage';
import MbieBrowsePage from './features/browse-mbie/MbieBrowsePage';
import LawaBrowsePage from './features/browse-lawa/LawaBrowsePage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/ask" element={<AskPage />} />
            <Route path="/results" element={<ResultsPage />} />
            <Route path="/browse/mbie" element={<MbieBrowsePage />} />
            <Route path="/browse/lawa" element={<LawaBrowsePage />} />
          </Routes>
        </Layout>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
