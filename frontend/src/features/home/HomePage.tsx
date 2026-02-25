import React, { useState } from 'react'
import { Link } from 'react-router-dom'

const HomePage: React.FC = () => {
  const [showHowItWorks, setShowHowItWorks] = useState(false)

  return (
    <div className="section-container">
      <div className="content-card bg-gradient-to-br from-slate-50 to-cyan-50 border-cyan-100">
        <p className="text-xs font-semibold tracking-[0.2em] uppercase text-cyan-700">
          Environmental Data Platform
        </p>
        <h1 className="text-4xl sm:text-5xl font-semibold text-neutral-900">
          Wai & Watts
        </h1>
        <p className="text-body text-neutral-700 max-w-3xl leading-relaxed">
          Explore grounded explanations built directly from published MBIE
          electricity generation and LAWA water quality datasets.
        </p>
        <p className="text-body text-neutral-700">
          Ask factual questions or browse structured data views.
        </p>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <div className="content-card border-blue-100 bg-blue-50/60">
          <h2 className="text-lg font-semibold text-blue-900">Ask Questions</h2>
          <p className="text-sm text-blue-900/90">
            Ask descriptive questions about electricity generation and water
            quality.
          </p>
          <p className="text-sm text-blue-900/90">
            Supported question shapes include trends over time, fuel
            comparisons, share-of-total metrics, and water quality state
            changes.
          </p>
          <p className="text-sm text-blue-900/90">
            All answers are grounded in stored dataset facts and include
            evidence references.
          </p>
          <div>
            <Link to="/ask" className="btn-primary inline-flex items-center">
              Ask a Question
            </Link>
          </div>
        </div>

        <div className="content-card border-emerald-100 bg-emerald-50/60">
          <h2 className="text-lg font-semibold text-emerald-900">
            Browse Data
          </h2>
          <p className="text-sm text-emerald-900/90">
            Inspect MBIE and LAWA datasets directly.
          </p>
          <p className="text-sm text-emerald-900/90">
            Annual and quarterly electricity generation, plus water quality
            state and trend slices.
          </p>
          <p className="text-sm text-emerald-900/90">
            Filter-first tables and charts help you inspect results quickly.
          </p>
          <div className="flex gap-2 flex-wrap">
            <Link
              to="/browse/mbie"
              className="inline-flex items-center px-3 py-2 text-sm rounded-md bg-emerald-700 text-white hover:bg-emerald-800"
            >
              MBIE Data
            </Link>
            <Link
              to="/browse/lawa"
              className="inline-flex items-center px-3 py-2 text-sm rounded-md bg-emerald-700 text-white hover:bg-emerald-800"
            >
              LAWA Data
            </Link>
          </div>
        </div>
      </div>

      <div className="pt-1">
        <button
          type="button"
          onClick={() => setShowHowItWorks(open => !open)}
          className="text-sm text-neutral-500 hover:text-neutral-700 underline underline-offset-4"
        >
          How explanations work →
        </button>
        {showHowItWorks && (
          <p className="mt-2 text-sm text-neutral-600 max-w-2xl">
            Questions are interpreted into supported analysis types, validated
            against available datasets, and answered from stored facts with
            evidence references. Requests outside scope receive guided
            alternatives.
          </p>
        )}
      </div>
    </div>
  )
}

export default HomePage
