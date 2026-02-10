import React from 'react'
import { Link } from 'react-router-dom'

const HomePage: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow rounded-lg p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">
          Wai & Watts: Environmental Data Platform
        </h1>

        <div className="prose max-w-none text-gray-700 mb-8">
          <p className="text-lg mb-6">
            Welcome to Wai & Watts, a platform for exploring New Zealand's
            environmental data through AI-powered explanations.
          </p>

          <p className="mb-6">
            Our system provides grounded explanations about electricity
            generation and water quality data, ensuring all responses are backed
            by verified sources and citations.
          </p>

          <div className="grid md:grid-cols-2 gap-6 mt-8">
            <div className="bg-blue-50 rounded-lg p-6">
              <h2 className="text-xl font-semibold text-blue-900 mb-3">
                Ask Questions
              </h2>
              <p className="text-blue-800 mb-4">
                Ask questions about New Zealand's electricity generation and
                water quality data. Our AI provides grounded explanations with
                citations.
              </p>
              <Link
                to="/ask"
                className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Ask Now
              </Link>
            </div>

            <div className="bg-green-50 rounded-lg p-6">
              <h2 className="text-xl font-semibold text-green-900 mb-3">
                Explore Data
              </h2>
              <p className="text-green-800 mb-4">
                Browse MBIE electricity generation and LAWA water quality data
                with filtering options.
              </p>
              <div className="space-y-2">
                <Link
                  to="/browse/mbie"
                  className="block inline-flex items-center px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 text-sm"
                >
                  MBIE Data
                </Link>
                <Link
                  to="/browse/lawa"
                  className="block inline-flex items-center px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 text-sm mt-2"
                >
                  LAWA Data
                </Link>
              </div>
            </div>
          </div>

          <div className="mt-8 p-4 bg-gray-50 rounded-lg">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              Key Features
            </h3>
            <ul className="text-sm text-gray-600 space-y-1">
              <li>• Grounded AI explanations with source citations</li>
              <li>• Real New Zealand environmental data</li>
              <li>• Deterministic refusal for unsupported queries</li>
              <li>• Table-first data exploration</li>
              <li>• Clean, modern UI</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}

export default HomePage
