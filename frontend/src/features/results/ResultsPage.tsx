import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import type { AskResult } from '../../types'
import { Card, CardContent, RefusalCallout } from '../../components/ui'
import { Button } from '../../components/ui'

const ResultsPage: React.FC = () => {
  const location = useLocation()
  const state = location.state as
    | { question?: string; explanation?: AskResult }
    | undefined

  return (
    <div className="section-container max-w-3xl mx-auto">
      {state?.question && (
        <Card className="mb-6">
          <CardContent>
            <h2 className="text-sm font-medium text-neutral-500 mb-2">
              Your Question
            </h2>
            <p className="text-neutral-900 text-lg">{state.question}</p>
          </CardContent>
        </Card>
      )}

      {state?.explanation?.isRefusal ? (
        <Card className="mb-6">
          <CardContent>
            <RefusalCallout
              message={
                state.explanation.refusal?.message ||
                'Unable to answer this question.'
              }
            />
            {state.explanation.refusal?.code && (
              <p className="text-sm text-neutral-500 mt-3">
                Code: {state.explanation.refusal.code}
              </p>
            )}
          </CardContent>
        </Card>
      ) : state?.explanation ? (
        <>
          <Card className="mb-6">
            <CardContent>
              <h3 className="text-lg font-semibold text-neutral-900 mb-4">
                Explanation
              </h3>
              {state.explanation.selectedDatasetSource && (
                <div className="mb-4 text-sm text-neutral-600">
                  <div className="font-medium text-neutral-700">
                    Using dataset: {state.explanation.selectedDatasetSource}
                  </div>
                  {state.explanation.datasetSelection?.reason && (
                    <div className="text-neutral-500 mt-1">
                      {state.explanation.datasetSelection.reason}
                    </div>
                  )}
                </div>
              )}
              <p className="text-neutral-700 leading-relaxed whitespace-pre-wrap">
                {state.explanation.explanation}
              </p>
            </CardContent>
          </Card>

          {state.explanation.citations &&
            state.explanation.citations.length > 0 && (
              <Card>
                <CardContent>
                  <h3 className="text-lg font-semibold text-neutral-900 mb-4">
                    Citations
                  </h3>
                  {state.explanation.citations.length === 0 ? (
                    <p className="text-neutral-600">No citations available</p>
                  ) : (
                    <ul className="space-y-3">
                      {state.explanation.citations.map((citation, index) => (
                        <li
                          key={index}
                          className="p-3 bg-neutral-50 rounded-lg border border-neutral-200"
                        >
                          <div className="font-medium text-neutral-800">
                            {citation.id}
                          </div>
                          {citation.type && (
                            <div className="text-sm text-neutral-600 mt-1">
                              <span className="font-medium">Type:</span>{' '}
                              {citation.type}
                            </div>
                          )}
                          {citation.field && (
                            <div className="text-sm text-neutral-600 mt-1">
                              <span className="font-medium">Field:</span>{' '}
                              {citation.field}
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </CardContent>
              </Card>
            )}
        </>
      ) : (
        <Card>
          <CardContent>
            <div className="text-center py-8">
              <div className="spinner w-8 h-8 mx-auto mb-4"></div>
              <p className="text-neutral-600">Processing your question...</p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="mt-6">
        <Link to="/ask">
          <Button variant="secondary">Ask another question</Button>
        </Link>
      </div>
    </div>
  )
}

export default ResultsPage
