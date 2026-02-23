import React from 'react'
import { Link } from 'react-router-dom'

const HomePage: React.FC = () => {
  return (
    <div className="section-container">
      <div className="content-card">
        <h1 className="text-h2 font-semibold text-neutral-900">
          Wai & Watts: Environmental Data Platform
        </h1>
        <p className="text-body text-neutral-600">
          Explore grounded explanations and browse persisted MBIE and LAWA
          datasets through a backend-authoritative workflow.
        </p>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <div className="content-card border-blue-100 bg-blue-50/60">
          <h2 className="text-lg font-semibold text-blue-900">Ask Questions</h2>
          <p className="text-sm text-blue-800">
            Ask descriptive questions about electricity generation and water
            quality. Responses include refusal-safe behavior and citations.
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
          <p className="text-sm text-emerald-800">
            Inspect MBIE annual/quarterly generation and LAWA state/trend slices
            with filter-first table and chart views.
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

      <div className="content-card">
        <h3 className="text-lg font-semibold text-neutral-900">
          What This Demonstrates
        </h3>
        <ul className="text-sm text-neutral-700 space-y-2">
          <li>
            - Grounded explanation pipeline with explicit citation surfaces
          </li>
          <li>
            - Deterministic refusal behavior for unsupported question shapes
          </li>
          <li>- Dataset lineage and release-aware ingestion semantics</li>
          <li>
            - Backend-authoritative domain logic with thin frontend rendering
          </li>
        </ul>
      </div>
    </div>
  )
}

export default HomePage
