import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAskQuestion } from '../../api/hooks';
import type { AskRequest } from '../../types';

const AskPage: React.FC = () => {
  const [question, setQuestion] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const askQuestion = useAskQuestion();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!question.trim()) {
      setError('Please enter a question');
      return;
    }

    setError(null);
    
    try {
      const request: AskRequest = { question: question.trim() };
      await askQuestion.mutateAsync(request);
      
      // Navigate to results page (will be implemented in PR 5)
      navigate('/results');
    } catch (err) {
      setError('Failed to process question. Please try again.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">
          Ask About Environmental Data
        </h1>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="question" className="block text-sm font-medium text-gray-700 mb-2">
              Your Question
            </label>
            <textarea
              id="question"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              rows={4}
              placeholder="e.g., Explain renewable generation trends between 2020 and 2023"
            />
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={askQuestion.isPending}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {askQuestion.isPending ? 'Processing...' : 'Ask Question'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AskPage;
