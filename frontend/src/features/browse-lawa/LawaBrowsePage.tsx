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
import { Card, CardContent, Button } from '../../components/ui'

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
    <div className="section-container">
      <div className="text-center mb-8">
        <h1 className="text-h2 font-semibold text-neutral-900 mb-3">
          LAWA Water Quality
        </h1>
        <p className="text-body text-neutral-600">
          Explore New Zealand's water quality state and trend data by region and
          indicator.
        </p>
      </div>

      <Card className="mb-6">
        <CardContent>
          <div className="flex flex-wrap gap-4 items-end">
            <div className="min-w-[140px]">
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                View Type
              </label>
              <select
                value={viewType}
                onChange={e => setViewType(e.target.value as 'state' | 'trend')}
                className="select-base"
              >
                <option value="state">State</option>
                <option value="trend">Trend</option>
              </select>
            </div>

            <div className="min-w-[160px]">
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Region
              </label>
              <select
                value={region}
                onChange={e => setRegion(e.target.value)}
                className="select-base"
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
                <div className="text-sm text-neutral-500 mt-1">
                  Loading regions...
                </div>
              )}
            </div>

            <div className="min-w-[180px]">
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Indicator
              </label>
              <select
                value={indicator}
                onChange={e => setIndicator(e.target.value)}
                className="select-base"
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
                <div className="text-sm text-neutral-500 mt-1">
                  Loading indicators...
                </div>
              )}
            </div>

            <Button onClick={handleExplainThis}>Explain This Data</Button>
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <div className="table-container overflow-x-auto">
        <table className="table-base">
          <thead>
            <tr className="table-header">
              <th className="px-6 py-3">Period</th>
              <th className="px-6 py-3">Region</th>
              <th className="px-6 py-3">Site</th>
              <th className="px-6 py-3">Indicator</th>
              <th className="px-6 py-3">
                {viewType === 'state' ? 'State' : 'Trend'}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100">
            {isLoading ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center">
                  <div className="spinner w-6 h-6 mx-auto mb-2"></div>
                  <p className="text-neutral-500 text-sm">Loading data...</p>
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center">
                  <p className="text-error-600 text-sm">
                    Error loading data: {error?.message || 'Unknown error'}
                  </p>
                </td>
              </tr>
            ) : !data || data.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center">
                  <p className="text-neutral-500 text-sm">No data available</p>
                </td>
              </tr>
            ) : (
              data.map(row => (
                <tr key={row.id} className="table-row">
                  <td className="table-cell font-medium">
                    {viewType === 'state'
                      ? `${row.periodStartYear}-${row.periodEndYear}`
                      : 'asOfYear' in row
                        ? `${row.asOfYear} (${row.periodStartYear}-${row.periodEndYear})`
                        : `${row.periodStartYear}-${row.periodEndYear}`}
                  </td>
                  <td className="table-cell">{row.region}</td>
                  <td className="table-cell">{row.siteName}</td>
                  <td className="table-cell">{row.indicatorRaw}</td>
                  <td className="table-cell">
                    <span
                      className={`px-2 py-1 text-xs rounded-full ${
                        viewType === 'state' && 'stateNorm' in row
                          ? getStateBadgeClass(row.stateNorm)
                          : viewType === 'trend' && 'trendNorm' in row
                            ? getTrendBadgeClass(row.trendNorm)
                            : 'bg-neutral-100 text-neutral-800'
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

      <div className="mt-4 text-sm text-neutral-600 text-right">
        {isLoading
          ? 'Loading data from backend...'
          : error
            ? 'Failed to load data from backend.'
            : `Showing ${data?.length || 0} records.`}
      </div>
    </div>
  )
}

export default LawaBrowsePage
