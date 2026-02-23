import React from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import type { AskResult } from '../../types'
import { Card, CardContent, RefusalCallout } from '../../components/ui'
import { Button } from '../../components/ui'
import { useCapabilities } from '../../api/hooks'

const getRefusalPresentation = (code?: string) => {
  if (code === 'INTERNAL_ERROR') {
    return {
      title: 'Not Supported Yet',
      variant: 'error' as const,
      fallbackMessage:
        'We hit a technical issue while processing your question. Please try again.',
    }
  }

  if (
    code === 'UNSUPPORTED_CAPABILITY' ||
    code === 'UNSUPPORTED_INTENT' ||
    code === 'DATASET_MISMATCH'
  ) {
    return {
      title: 'Outside Supported Scope',
      variant: 'warning' as const,
      fallbackMessage:
        'Wai & Watts explains published historical data and cannot answer this request as written.',
    }
  }

  return {
    title: 'Not Supported Yet',
    variant: 'warning' as const,
    fallbackMessage:
      'This request is not supported by the current capability set.',
  }
}

const toSentenceChunks = (text: string): string[] =>
  text
    .split(/[\n.]/)
    .map(chunk => chunk.trim())
    .filter(Boolean)

const extractKeyFigures = (result: AskResult): string[] => {
  const summary = result.dataSummary
  if (summary && Object.keys(summary).length > 0) {
    return Object.entries(summary)
      .filter(
        ([, value]) => typeof value === 'number' || typeof value === 'string'
      )
      .slice(0, 8)
      .map(([key, value]) => `${key}: ${String(value)}`)
  }

  return toSentenceChunks(result.explanation || '')
    .filter(chunk => /\d/.test(chunk))
    .slice(0, 6)
}

const parseExampleList = (value: unknown): string[] => {
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .filter(item => typeof item === 'string')
    .map(item => item.trim())
    .filter(Boolean)
    .slice(0, 5)
}

const datasetLabel = (datasetCode: string): string =>
  ({
    'mbie.generation.annual': 'MBIE Electricity Generation - Annual Dataset',
    'mbie.generation.quarterly':
      'MBIE Electricity Generation - Quarterly Dataset',
    'lawa.water_quality.state.multi_year':
      'LAWA Water Quality - State Multi-Year Dataset',
    'lawa.water_quality.trend.multi_year':
      'LAWA Water Quality - Trend Multi-Year Dataset',
  })[datasetCode] || datasetCode

const ResultsPage: React.FC = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const capabilities = useCapabilities()
  const state = location.state as
    | { question?: string; explanation?: AskResult }
    | undefined
  const refusalCode = state?.explanation?.refusal?.code
  const refusalPresentation = getRefusalPresentation(refusalCode)
  const parserUsed = state?.explanation?.debug?.parserUsed
  const isDemoMode = parserUsed === 'DEMO'
  const isDemoFallback = parserUsed === 'LLM_FALLBACK_DEMO'
  const capabilityDatasets = capabilities.data?.datasets || []
  const datasetNameByCode = new Map(
    capabilityDatasets.map(dataset => [
      dataset.datasetSource,
      dataset.displayName,
    ])
  )
  const selectedDatasetCode = state?.explanation?.selectedDatasetSource
  const selectedDatasetName = selectedDatasetCode
    ? datasetLabel(
        datasetNameByCode.get(selectedDatasetCode) || selectedDatasetCode
      )
    : null
  const keyFigures = state?.explanation
    ? extractKeyFigures(state.explanation)
    : []
  const refusalExamples = state?.explanation?.refusal?.details
    ? parseExampleList(state.explanation.refusal.details.examples)
    : []
  const fallbackExamples = (capabilities.data?.capabilities || [])
    .flatMap(capability => capability.examples || [])
    .slice(0, 5)
  const guidedExamples =
    refusalExamples.length > 0 ? refusalExamples : fallbackExamples

  return (
    <div className="section-container max-w-3xl mx-auto">
      {(isDemoMode || isDemoFallback) && (
        <Card className="mb-6">
          <CardContent>
            <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3">
              <p className="text-sm font-medium text-amber-900">
                {isDemoMode
                  ? 'Demo mode active (LLM not configured)'
                  : 'LLM fallback mode active'}
              </p>
              <p className="text-sm text-amber-800 mt-1">
                {isDemoMode
                  ? 'This answer was produced using the deterministic demo parser/provider path, not a live LLM call.'
                  : 'The live LLM parser was unavailable, so the request used deterministic demo fallback behavior.'}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {state?.question && (
        <Card className="mb-6">
          <CardContent>
            <h2 className="text-sm font-medium text-neutral-500 mb-2">
              Question
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
                refusalPresentation.fallbackMessage
              }
              title={refusalPresentation.title}
              variant={refusalPresentation.variant}
            />
            <div className="mt-4 text-sm text-neutral-700">
              Wai & Watts explains published historical data. It does not
              provide predictions, causal claims, or recommendations.
            </div>
            {guidedExamples.length > 0 && (
              <div className="mt-4">
                <h4 className="text-sm font-semibold text-neutral-800 mb-2">
                  You can ask:
                </h4>
                <div className="flex flex-wrap gap-2">
                  {guidedExamples.map(example => (
                    <button
                      key={example}
                      type="button"
                      onClick={() =>
                        navigate('/ask', { state: { seedQuestion: example } })
                      }
                      className="px-3 py-2 rounded-full border border-neutral-300 bg-white text-sm text-neutral-700 hover:border-primary-300 hover:text-primary-700"
                    >
                      {example}
                    </button>
                  ))}
                </div>
              </div>
            )}
            <details className="mt-4 rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2">
              <summary className="cursor-pointer text-sm font-medium text-neutral-700">
                Technical details
              </summary>
              <div className="mt-2 text-sm text-neutral-600">
                {state.explanation.refusal?.code && (
                  <div>Code: {state.explanation.refusal.code}</div>
                )}
                {state.explanation.debug?.refusalTrigger && (
                  <div>Trigger: {state.explanation.debug.refusalTrigger}</div>
                )}
              </div>
            </details>
          </CardContent>
        </Card>
      ) : state?.explanation ? (
        <>
          <Card className="mb-6">
            <CardContent>
              <h3 className="text-lg font-semibold text-neutral-900 mb-4">
                Answer
              </h3>
              <p className="text-neutral-700 leading-relaxed whitespace-pre-wrap">
                {state.explanation.explanation}
              </p>
            </CardContent>
          </Card>

          <Card className="mb-6">
            <CardContent>
              <h3 className="text-lg font-semibold text-neutral-900 mb-3">
                Key Figures
              </h3>
              {keyFigures.length > 0 ? (
                <ul className="list-disc pl-5 space-y-1 text-neutral-700">
                  {keyFigures.map(figure => (
                    <li key={figure}>{figure}</li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-neutral-600">
                  Key numeric outputs were not available for this answer.
                </p>
              )}
            </CardContent>
          </Card>

          <Card className="mb-6">
            <CardContent>
              <h3 className="text-lg font-semibold text-neutral-900 mb-3">
                Data Source
              </h3>
              <p className="text-neutral-700">
                Source:{' '}
                {selectedDatasetName || 'Dataset selected during processing'}
              </p>
              {state.explanation.parsedRequest?.filters &&
                (state.explanation.parsedRequest.filters.startYear ||
                  state.explanation.parsedRequest.filters.endYear) && (
                  <p className="text-sm text-neutral-600 mt-1">
                    Time range:{' '}
                    {state.explanation.parsedRequest.filters.startYear ||
                      'start'}{' '}
                    -{' '}
                    {state.explanation.parsedRequest.filters.endYear ||
                      'latest'}
                  </p>
                )}
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <h3 className="text-lg font-semibold text-neutral-900 mb-2">
                Evidence
              </h3>
              <details className="rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2">
                <summary className="cursor-pointer text-sm font-medium text-neutral-700">
                  Show evidence details
                </summary>
                <div className="mt-2 space-y-1 text-sm text-neutral-600">
                  <div>
                    Field used:{' '}
                    {state.explanation.citations?.find(
                      citation => citation.field
                    )?.field || 'N/A'}
                  </div>
                  <div>
                    Time range:{' '}
                    {state.explanation.parsedRequest?.filters?.startYear ||
                      'start'}{' '}
                    -{' '}
                    {state.explanation.parsedRequest?.filters?.endYear ||
                      'latest'}
                  </div>
                  <div>Dataset: {selectedDatasetName || 'N/A'}</div>
                </div>

                <details className="mt-3 rounded-md border border-neutral-200 bg-white px-3 py-2">
                  <summary className="cursor-pointer text-sm font-medium text-neutral-700">
                    Technical fact identifiers
                  </summary>
                  {state.explanation.citations?.length ? (
                    <ul className="mt-2 space-y-2">
                      {state.explanation.citations.map(citation => (
                        <li
                          key={citation.id}
                          className="rounded border border-neutral-200 px-2 py-1 text-xs text-neutral-600"
                        >
                          {citation.id}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="mt-2 text-sm text-neutral-600">
                      No technical citation identifiers were returned.
                    </p>
                  )}
                </details>
              </details>
            </CardContent>
          </Card>
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
