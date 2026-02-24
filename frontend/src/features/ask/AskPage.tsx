import React, { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAskQuestion, useCapabilities } from '../../api/hooks'
import type { AskRequest, CapabilityDefinition } from '../../types'

const promptValueByToken: Record<string, string> = {
  fuelType: 'hydro',
  fuelTypeB: 'wind',
  stateCategory: 'EXCELLENT',
  indicator: 'E. coli',
  region: 'Auckland',
  trend: 'improving',
}

const buildPromptFromTemplate = (template: string) => {
  const currentYear = new Date().getFullYear()
  const replacements: Record<string, string> = {
    startYear: String(currentYear - 5),
    endYear: String(currentYear - 1),
    ...promptValueByToken,
  }

  return template.replace(/\{(\w+)\}/g, (_, token: string) => {
    return replacements[token] || token
  })
}

const capabilityLabel = (capability: CapabilityDefinition) =>
  ({
    fuel_generation_trend: 'Fuel Trend Over Time',
    fuel_type_comparison: 'Compare Two Fuel Types',
    regional_water_quality: 'Compare Regions',
    water_quality_state_sites_trend: 'Track Site State Over Time',
  })[capability.questionType] ||
  capability.displayName ||
  capability.description

const datasetLabel = (datasetCode: string, fallback?: string) =>
  ({
    'mbie.generation.annual': 'Electricity Generation (Annual Data)',
    'mbie.generation.quarterly': 'Electricity Generation (Quarterly Data)',
    'lawa.water_quality.state.multi_year': 'Water Quality (State View)',
    'lawa.water_quality.trend.multi_year': 'Water Quality (Trend View)',
  })[datasetCode] ||
  fallback ||
  datasetCode

const AskPage: React.FC = () => {
  const location = useLocation()
  const initialQuestion =
    (location.state as { seedQuestion?: string } | undefined)?.seedQuestion ||
    ''
  const [question, setQuestion] = useState(initialQuestion.trim())
  const [error, setError] = useState<string | null>(null)
  const [selectedIntent, setSelectedIntent] = useState<string | null>(null)
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

  const capabilityList = useMemo(
    () => capabilities.data?.capabilities ?? [],
    [capabilities.data?.capabilities]
  )
  const datasets = useMemo(
    () => capabilities.data?.datasets ?? [],
    [capabilities.data?.datasets]
  )

  const datasetNames = useMemo(() => {
    const map = new Map<string, string>()
    datasets.forEach(dataset =>
      map.set(dataset.datasetSource, dataset.displayName)
    )
    return map
  }, [datasets])

  const groupedCapabilities = useMemo(() => {
    const grouped = new Map<string, CapabilityDefinition[]>()
    capabilityList.forEach(capability => {
      const datasetCodes =
        capability.supportedDatasets || capability.datasetSources
      datasetCodes.forEach(datasetCode => {
        if (!grouped.has(datasetCode)) {
          grouped.set(datasetCode, [])
        }
        const existing = grouped.get(datasetCode)!
        if (
          !existing.find(item => item.questionType === capability.questionType)
        ) {
          existing.push(capability)
        }
      })
    })
    return Array.from(grouped.entries())
  }, [capabilityList])

  const selectedCapability = capabilityList.find(
    capability => capability.questionType === selectedIntent
  )

  const selectedExamples = useMemo(() => {
    if (!selectedCapability) {
      return []
    }
    const fromTemplates = (selectedCapability.exampleTemplates || []).map(
      buildPromptFromTemplate
    )
    const combined = [...fromTemplates, ...(selectedCapability.examples || [])]
    return Array.from(new Set(combined)).slice(0, 4)
  }, [selectedCapability])

  const globalExamples = useMemo(() => {
    if (selectedExamples.length > 0) {
      return selectedExamples
    }
    const flattened = capabilityList.flatMap(capability => {
      const fromTemplates = (capability.exampleTemplates || []).map(
        buildPromptFromTemplate
      )
      return [...fromTemplates, ...(capability.examples || [])]
    })
    return Array.from(new Set(flattened))
  }, [capabilityList, selectedExamples])

  const rotatingExamples = useMemo(() => {
    const examples = globalExamples.length > 0 ? globalExamples : []
    if (examples.length <= 4) {
      return examples
    }
    const dayOffset = new Date().getDate() % examples.length
    return Array.from({ length: 4 }, (_, index) => {
      return examples[(dayOffset + index) % examples.length]
    })
  }, [globalExamples])

  return (
    <div className="section-container">
      {/* Header */}
      <div className="text-center mb-8">
        <h1 className="text-h2 font-semibold text-neutral-900 mb-3">
          Ask a Question
        </h1>
        <p className="text-body text-neutral-600 max-w-2xl mx-auto">
          Get grounded answers from MBIE electricity generation and LAWA water
          quality datasets.
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
              placeholder="e.g., How has wind generation changed over time?"
            />
            {!question.trim() && (
              <p className="text-xs text-neutral-500 mt-2">
                Try asking about generation trends, fuel comparisons, or water
                quality state changes.
              </p>
            )}
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

      <div className="content-card mt-6">
        <h3 className="text-lg font-medium text-neutral-900 mb-3">
          Start with a Question
        </h3>
        <p className="text-sm text-neutral-600 mb-4">
          Pick a capability to reveal example prompts you can ask directly.
        </p>
        {capabilities.isLoading ? (
          <p className="text-sm text-neutral-500">Loading capabilities...</p>
        ) : (
          <div className="space-y-4">
            {groupedCapabilities.map(([datasetCode, capabilityGroup]) => (
              <div key={datasetCode}>
                <h4 className="text-sm font-semibold text-neutral-800 mb-2">
                  {datasetLabel(datasetCode, datasetNames.get(datasetCode))}
                </h4>
                <div className="flex flex-wrap gap-2">
                  {capabilityGroup.map(capability => {
                    const isSelected =
                      selectedIntent === capability.questionType
                    return (
                      <button
                        key={capability.questionType}
                        type="button"
                        onClick={() =>
                          setSelectedIntent(
                            isSelected ? null : capability.questionType
                          )
                        }
                        className={`px-3 py-2 rounded-full border text-sm transition-colors ${
                          isSelected
                            ? 'border-primary-500 bg-primary-100 text-primary-800'
                            : 'border-neutral-300 bg-white text-neutral-700 hover:border-primary-300 hover:text-primary-700'
                        }`}
                      >
                        {capabilityLabel(capability)}
                      </button>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Example Questions */}
      <div className="mt-8">
        <h3 className="text-lg font-medium text-neutral-900 mb-4">
          Try asking:
        </h3>
        {selectedCapability && (
          <p className="text-sm text-neutral-600 mb-3">
            Showing examples for {capabilityLabel(selectedCapability)}.
          </p>
        )}
        <div className="space-y-2">
          {rotatingExamples.map((example, index) => (
            <button
              key={index}
              type="button"
              onClick={() => setQuestion(example)}
              className="w-full text-left p-3 border border-neutral-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-all duration-200 group"
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
