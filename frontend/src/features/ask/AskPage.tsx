import { type SyntheticEvent, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAskQuestion, useCapabilities } from '../../api/hooks'
import type {
  AskRequest,
  CapabilityDefinition,
  CapabilitiesResponse,
} from '../../types'

type CapabilitiesResponseWithSuggestions = CapabilitiesResponse & {
  suggestedValuesByToken?: Record<string, string[]>
}

const fallbackSuggestedValuesByToken: Record<string, string[]> = {
  fuelType: ['wind', 'solar', 'hydro', 'geothermal'],
  fuelTypeB: ['hydro', 'wind', 'solar', 'coal'],
  stateCategory: ['EXCELLENT', 'GOOD', 'FAIR', 'POOR'],
  indicator: ['E. coli', 'Nitrate', 'Ammoniacal nitrogen'],
  region: ['Auckland', 'Canterbury', 'Otago', 'Waikato'],
  trend: ['improving', 'declining', 'stable'],
}

const tokenHash = (token: string) =>
  token.split('').reduce((total, char) => total + char.charCodeAt(0), 0)

const buildPromptFromTemplate = (
  template: string,
  suggestedValuesByToken: Record<string, string[]>,
  seed: number
) => {
  const currentYear = new Date().getFullYear()
  const replacements: Record<string, string> = {
    startYear: String(currentYear - 5),
    endYear: String(currentYear - 1),
  }

  return template.replace(/{(\w+)}/g, (_, token: string) => {
    if (replacements[token]) {
      return replacements[token]
    }
    const suggestedValues = suggestedValuesByToken[token] || []
    if (suggestedValues.length === 0) {
      return token
    }
    const index = (seed + tokenHash(token)) % suggestedValues.length
    return suggestedValues[index]
  })
}

const AskPage = () => {
  const location = useLocation()
  const initialQuestion =
    (location.state as { seedQuestion?: string } | undefined)?.seedQuestion ||
    ''
  const [question, setQuestion] = useState(initialQuestion.trim())
  const [error, setError] = useState<string | null>(null)
  const [selectedIntent, setSelectedIntent] = useState<string | null>(null)
  const questionInputRef = useRef<HTMLTextAreaElement>(null)
  const navigate = useNavigate()

  const askQuestion = useAskQuestion()
  const capabilities = useCapabilities()
  const capabilitiesData: CapabilitiesResponseWithSuggestions | undefined =
    capabilities.data as CapabilitiesResponseWithSuggestions | undefined

  const submitQuestion = async (rawQuestion: string) => {
    const trimmedQuestion = rawQuestion.trim()
    if (!trimmedQuestion) {
      setError('Please enter a question')
      return
    }

    setError(null)

    try {
      const request: AskRequest = { question: trimmedQuestion }
      const result = await askQuestion.mutateAsync(request)

      // Navigate to results page with response data
      navigate('/results', {
        state: {
          question: trimmedQuestion,
          explanation: result,
        },
      })
    } catch {
      setError(
        'We hit a technical issue while contacting the explanation service. Please try again.'
      )
    }
  }

  const handleSubmit = async (
    e: SyntheticEvent<HTMLFormElement, SubmitEvent>
  ) => {
    e.preventDefault()
    await submitQuestion(question)
  }

  const handleExampleClick = (example: string) => {
    setQuestion(example)
    requestAnimationFrame(() => {
      questionInputRef.current?.focus()
      questionInputRef.current?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      })
    })
    void submitQuestion(example)
  }

  const capabilityList = useMemo(
    () => capabilitiesData?.capabilities ?? [],
    [capabilitiesData?.capabilities]
  )
  const datasets = useMemo(
    () => capabilitiesData?.datasets ?? [],
    [capabilitiesData?.datasets]
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
  const suggestedValuesByToken = useMemo(() => {
    return {
      ...fallbackSuggestedValuesByToken,
      ...(capabilitiesData?.suggestedValuesByToken || {}),
    }
  }, [capabilitiesData?.suggestedValuesByToken])
  const daySeed = useMemo(() => new Date().getDate(), [])

  const selectedExamples = useMemo(() => {
    if (!selectedCapability) {
      return []
    }
    const fromTemplates = (selectedCapability.exampleTemplates || []).map(
      (template, index) =>
        buildPromptFromTemplate(
          template,
          suggestedValuesByToken,
          daySeed + index
        )
    )
    const combined = [...fromTemplates, ...(selectedCapability.examples || [])]
    return Array.from(new Set(combined)).slice(0, 4)
  }, [daySeed, selectedCapability, suggestedValuesByToken])

  const globalExamples = useMemo(() => {
    if (selectedExamples.length > 0) {
      return selectedExamples
    }
    let templateIndex = 0
    const flattened = capabilityList.flatMap(capability => {
      const fromTemplates = (capability.exampleTemplates || []).map(template =>
        buildPromptFromTemplate(
          template,
          suggestedValuesByToken,
          daySeed + templateIndex++
        )
      )
      return [...fromTemplates, ...(capability.examples || [])]
    })
    return Array.from(new Set(flattened))
  }, [capabilityList, daySeed, selectedExamples, suggestedValuesByToken])

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
              ref={questionInputRef}
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
                  {datasetNames.get(datasetCode) || datasetCode}
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
                        {capability.displayName ||
                          capability.description ||
                          capability.questionType}
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
            Showing examples for{' '}
            {selectedCapability.displayName ||
              selectedCapability.description ||
              selectedCapability.questionType}
            .
          </p>
        )}
        <div className="space-y-2">
          {rotatingExamples.map((example, index) => (
            <button
              key={index}
              type="button"
              onClick={() => handleExampleClick(example)}
              disabled={askQuestion.isPending}
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
