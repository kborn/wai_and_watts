import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAskQuestion } from '../../api/hooks'
import type { AskRequest } from '../../types'

const AskPage: React.FC = () => {
  const [question, setQuestion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const askQuestion = useAskQuestion()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!question.trim()) {
      setError('Please enter a question')
      return
    }

    setError(null)

    try {
      const request: AskRequest = { question: question.trim() }
      const result = await askQuestion.mutateAsync(request)

      // Navigate to results page with response data
      navigate('/results', {
        state: {
          question: question.trim(),
          explanation: result,
        },
      })
    } catch {
      setError('Failed to process question. Please try again.')
    }
  }

  const exampleQuestions = [
    'Explain renewable generation trends between 2020 and 2023',
    'What are the main sources of electricity generation in New Zealand?',
    'How has wind energy generation changed over the last 5 years?',
    'Compare hydro and geothermal generation patterns',
  ]

  return (
    <div className="section-container">
      {/* Header */}
      <div className="text-center mb-8">
        <h1 className="text-h2 font-semibold text-neutral-900 mb-3">
          Ask About Environmental Data
        </h1>
        <p className="text-body text-neutral-600 max-w-2xl mx-auto">
          Get insights about New Zealand's electricity generation and water
          quality data from authoritative sources.
        </p>
      </div>

      {/* Main Form */}
      <div className="content-card">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label
              htmlFor="question"
              className="block text-sm font-medium text-neutral-700 mb-2"
            >
              Your Question
            </label>
            <textarea
              id="question"
              value={question}
              onChange={e => setQuestion(e.target.value)}
              className="input-base resize-none"
              rows={4}
              placeholder="e.g., Explain renewable generation trends between 2020 and 2023"
            />
          </div>

          {error && <div className="callout-error">{error}</div>}

          <button
            type="submit"
            disabled={askQuestion.isPending}
            className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {askQuestion.isPending ? (
              <>
                <div className="spinner w-4 h-4"></div>
                Processing...
              </>
            ) : (
              'Ask Question'
            )}
          </button>
        </form>
      </div>

      {/* Example Questions */}
      <div className="mt-8">
        <h3 className="text-lg font-medium text-neutral-900 mb-4">
          Example Questions
        </h3>
        <div className="grid gap-3 sm:grid-cols-2">
          {exampleQuestions.map((example, index) => (
            <button
              key={index}
              onClick={() => setQuestion(example)}
              className="text-left p-4 border border-neutral-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-all duration-200 group"
            >
              <p className="text-sm text-neutral-700 group-hover:text-primary-700">
                {example}
              </p>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

export default AskPage
