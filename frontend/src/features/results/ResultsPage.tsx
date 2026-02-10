import React from 'react'
import { useLocation } from 'react-router-dom'
import type { Explanation } from '../../types'

const ResultsPage: React.FC = () => {
  const location = useLocation()
  const state = location.state as
    | { question?: string; explanation?: Explanation }
    | undefined

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6">
        {state?.question && (
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">
              Your Question:
            </h2>
            <p className="text-gray-700 bg-gray-50 p-3 rounded">
              {state.question}
            </p>
          </div>
        )}

        {state?.explanation ? (
          <>
            <div className="mb-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-3">
                Explanation
              </h3>
              <div className="prose max-w-none">
                <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {state.explanation.explanation}
                </p>
              </div>
            </div>

            {state.explanation.citations &&
              state.explanation.citations.length > 0 && (
                <div className="mb-6 bg-gray-50 rounded-lg p-4">
                  <h3 className="text-lg font-semibold text-gray-900 mb-3">
                    Citations
                  </h3>
                  {state.explanation.citations.length === 0 ? (
                    <p className="text-gray-600">No citations available</p>
                  ) : (
                    <ul className="space-y-2">
                      {state.explanation.citations.map((citation, index) => (
                        <li
                          key={index}
                          className="text-sm text-gray-700 border-l-4 border-blue-400 pl-3"
                        >
                          <div className="font-medium">{citation.dataset}</div>
                          {citation.field && (
                            <div className="text-gray-600">
                              Field: {citation.field}
                            </div>
                          )}
                          {citation.value && (
                            <div className="text-gray-600">
                              Value: {citation.value}
                            </div>
                          )}
                          {citation.source && (
                            <div className="text-gray-500">
                              Source: {citation.source}
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
          </>
        ) : state?.explanation?.refusalCategory ? (
          <div className="border-l-4 border-red-400 bg-red-50 p-4 rounded">
            <h2 className="text-lg font-semibold text-red-800 mb-2">
              Question Not Supported
            </h2>
            <p className="text-red-700 mb-1">
              {state.explanation.refusalReason}
            </p>
            <p className="text-sm text-red-600">
              Category: {state.explanation.refusalCategory}
            </p>
          </div>
        ) : (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Processing your question...</p>
          </div>
        )}

        <div className="mt-6 pt-6 border-t border-gray-200">
          <a
            href="/ask"
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-blue-700 bg-blue-100 hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Ask another question
          </a>
        </div>
      </div>
    </div>
  )
}

export default ResultsPage
