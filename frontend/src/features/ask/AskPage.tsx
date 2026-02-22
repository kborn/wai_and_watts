import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAskQuestion, useCapabilities } from '../../api/hooks'
import type { AskRequest } from '../../types'

const AskPage: React.FC = () => {
  const [question, setQuestion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const askQuestion = useAskQuestion()
  const capabilities = useCapabilities()

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
      setError(
        'We hit a technical issue while contacting the explanation service. Please try again.'
      )
    }
  }

  const exampleQuestions = [
    'Explain renewable generation trends between 2020 and 2023',
    'What are the main sources of electricity generation in New Zealand?',
    'Compare hydro and geothermal generation patterns',
    'Explain hydro generation trends between 2018 and 2023',
  ]

  const supportedQuestions = Object.values(
    capabilities.data?.supportedQuestionTypes || {}
  )
  const unsupportedQuestions = Object.values(
    capabilities.data?.unsupportedQuestionTypes || {}
  )
  const supportedDatasets = Object.values(
    capabilities.data?.supportedDatasetSources || {}
  )

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

      <div className="content-card mb-6">
        <h3 className="text-lg font-semibold text-neutral-900 mb-2">
          What You Can Ask Here
        </h3>
        <p className="text-sm text-neutral-600 mb-3">
          Ask factual questions about MBIE electricity generation and LAWA water
          quality datasets. Responses are grounded in stored dataset facts.
        </p>
        <details className="rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2">
          <summary className="cursor-pointer text-sm font-medium text-neutral-700">
            Supported scope and limits
          </summary>
          <div className="mt-3 space-y-3 text-sm text-neutral-700">
            <div>
              <div className="font-medium text-neutral-800">Data scope</div>
              {capabilities.isLoading ? (
                <div className="text-neutral-500 mt-1">Loading scope...</div>
              ) : supportedDatasets.length > 0 ? (
                <ul className="list-disc pl-5 mt-1 space-y-1">
                  {supportedDatasets.map(dataset => (
                    <li key={dataset}>{dataset}</li>
                  ))}
                </ul>
              ) : (
                <div className="text-neutral-500 mt-1">
                  MBIE generation and LAWA water quality datasets
                </div>
              )}
            </div>

            <div>
              <div className="font-medium text-neutral-800">
                Question types currently supported
              </div>
              {capabilities.isLoading ? (
                <div className="text-neutral-500 mt-1">
                  Loading supported question types...
                </div>
              ) : supportedQuestions.length > 0 ? (
                <ul className="list-disc pl-5 mt-1 space-y-1">
                  {supportedQuestions.slice(0, 6).map(questionType => (
                    <li key={questionType}>{questionType}</li>
                  ))}
                </ul>
              ) : (
                <div className="text-neutral-500 mt-1">
                  Trend, comparison, and overview questions over available
                  datasets
                </div>
              )}
            </div>

            <div>
              <div className="font-medium text-neutral-800">
                Not currently supported
              </div>
              {capabilities.isLoading ? (
                <div className="text-neutral-500 mt-1">
                  Loading unsupported categories...
                </div>
              ) : unsupportedQuestions.length > 0 ? (
                <ul className="list-disc pl-5 mt-1 space-y-1">
                  {unsupportedQuestions.slice(0, 5).map(questionType => (
                    <li key={questionType}>{questionType}</li>
                  ))}
                </ul>
              ) : (
                <div className="text-neutral-500 mt-1">
                  Forecasting, causation, policy recommendations, and
                  hypothetical scenarios
                </div>
              )}
            </div>
          </div>
        </details>
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
