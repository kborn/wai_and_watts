import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useLawaStateMultiYear,
  useLawaTrendMultiYear,
  useLawaStateRegions,
  useLawaStateIndicators,
  useLawaTrendRegions,
  useLawaTrendIndicators,
} from '../../api/hooks'

const LawaBrowsePage: React.FC = () => {
  const [viewType, setViewType] = useState<'state' | 'trend'>('state')
  const [region, setRegion] = useState('')
  const [indicator, setIndicator] = useState('')
  const navigate = useNavigate()

  // Dynamic filter options
  const stateRegions = useLawaStateRegions()
  const stateIndicators = useLawaStateIndicators()
  const trendRegions = useLawaTrendRegions()
  const trendIndicators = useLawaTrendIndicators()

  const stateData = useLawaStateMultiYear({
    region: region || undefined,
    indicator: indicator || undefined,
  })
  const trendData = useLawaTrendMultiYear({
    region: region || undefined,
    indicator: indicator || undefined,
  })

  const data = viewType === 'state' ? stateData.data : trendData.data
  const isLoading =
    viewType === 'state' ? stateData.isLoading : trendData.isLoading
  const error = viewType === 'state' ? stateData.error : trendData.error

  const handleExplainThis = () => {
    const context =
      viewType === 'state'
        ? 'Explain water quality state data'
        : 'Explain water quality trend data'

    const filters = []
    if (region) filters.push(`in ${region}`)
    if (indicator) filters.push(`for ${indicator}`)

    const question =
      filters.length > 0 ? `${context} ${filters.join(' ')}` : context

    navigate('/ask', { state: { prefill: question } })
  }

  const getStateBadgeClass = (stateNorm: string) => {
    switch (stateNorm) {
      case 'EXCELLENT':
        return 'bg-green-100 text-green-800'
      case 'GOOD':
        return 'bg-blue-100 text-blue-800'
      case 'FAIR':
        return 'bg-yellow-100 text-yellow-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getTrendBadgeClass = (trendNorm: string) => {
    switch (trendNorm) {
      case 'IMPROVING':
        return 'bg-green-100 text-green-800'
      case 'DEGRADING':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          LAWA Water Quality Data
        </h1>

        <div className="flex flex-wrap gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              View Type
            </label>
            <select
              value={viewType}
              onChange={e => setViewType(e.target.value as 'state' | 'trend')}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="state">State</option>
              <option value="trend">Trend</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Region
            </label>
            <select
              value={region}
              onChange={e => setRegion(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              disabled={
                viewType === 'state'
                  ? stateRegions.isLoading
                  : trendRegions.isLoading
              }
            >
              <option value="">All</option>
              {(viewType === 'state'
                ? stateRegions.data
                : trendRegions.data
              )?.map(region => (
                <option key={region} value={region}>
                  {region}
                </option>
              ))}
            </select>
            {(viewType === 'state'
              ? stateRegions.isLoading
              : trendRegions.isLoading) && (
              <div className="text-sm text-gray-500 mt-1">
                Loading regions...
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Indicator
            </label>
            <select
              value={indicator}
              onChange={e => setIndicator(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              disabled={
                viewType === 'state'
                  ? stateIndicators.isLoading
                  : trendIndicators.isLoading
              }
            >
              <option value="">All</option>
              {(viewType === 'state'
                ? stateIndicators.data
                : trendIndicators.data
              )?.map(indicator => (
                <option key={indicator} value={indicator}>
                  {indicator}
                </option>
              ))}
            </select>
            {(viewType === 'state'
              ? stateIndicators.isLoading
              : trendIndicators.isLoading) && (
              <div className="text-sm text-gray-500 mt-1">
                Loading indicators...
              </div>
            )}
          </div>

          <div className="flex items-end">
            <button
              onClick={handleExplainThis}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Explain This Data
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Period
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Region
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Site
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Indicator
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {viewType === 'state' ? 'State' : 'Trend'}
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-4 text-center text-gray-500"
                  >
                    Loading data...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-4 text-center text-red-500"
                  >
                    Error loading data: {error?.message || 'Unknown error'}
                  </td>
                </tr>
              ) : !data || data.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-6 py-4 text-center text-gray-500"
                  >
                    No data available
                  </td>
                </tr>
              ) : (
                data.map(row => (
                  <tr key={row.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {viewType === 'state'
                        ? `${row.periodStartYear}-${row.periodEndYear}`
                        : 'asOfYear' in row
                          ? `${row.asOfYear} (${row.periodStartYear}-${row.periodEndYear})`
                          : `${row.periodStartYear}-${row.periodEndYear}`}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {row.region}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {row.siteName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {row.indicatorRaw}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <span
                        className={`px-2 py-1 text-xs rounded-full ${
                          viewType === 'state' && 'stateNorm' in row
                            ? getStateBadgeClass(row.stateNorm)
                            : viewType === 'trend' && 'trendNorm' in row
                              ? getTrendBadgeClass(row.trendNorm)
                              : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {viewType === 'state' && 'stateNorm' in row
                          ? row.stateNorm
                          : viewType === 'trend' && 'trendNorm' in row
                            ? row.trendNorm
                            : 'UNKNOWN'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-4 text-sm text-gray-600">
          {isLoading
            ? 'Loading data from backend...'
            : error
              ? 'Failed to load data from backend.'
              : `Showing ${data?.length || 0} records.`}
        </div>
      </div>
    </div>
  )
}

export default LawaBrowsePage
