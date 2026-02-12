import React, { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useMbieGenerationAnnual,
  useMbieGenerationQuarterly,
  useMbieGenerationAnnualFuelTypes,
  useMbieGenerationQuarterlyFuelTypes,
} from '../../api/hooks'
import type {
  MbieGenerationAnnualRecord,
  MbieGenerationQuarterlyRecord,
} from '../../types'
import { BarChart } from '../../components/ui'

const MbieBrowsePage: React.FC = () => {
  const [viewType, setViewType] = useState<'annual' | 'quarterly'>('annual')
  const [fuelType, setFuelType] = useState('')
  const navigate = useNavigate()

  // Dynamic fuel type options
  const annualFuelTypes = useMbieGenerationAnnualFuelTypes()
  const quarterlyFuelTypes = useMbieGenerationQuarterlyFuelTypes()

  const annualData = useMbieGenerationAnnual({
    fuelType: fuelType || undefined,
  })
  const quarterlyData = useMbieGenerationQuarterly({
    fuelType: fuelType || undefined,
  })

  const annualRecords = annualData.data || []
  const quarterlyRecords = quarterlyData.data || []
  const data = viewType === 'annual' ? annualRecords : quarterlyRecords
  const isLoading =
    viewType === 'annual' ? annualData.isLoading : quarterlyData.isLoading
  const error = viewType === 'annual' ? annualData.error : quarterlyData.error

  const handleExplainThis = () => {
    const context =
      viewType === 'annual'
        ? 'Explain annual electricity generation data'
        : 'Explain quarterly electricity generation data'

    if (fuelType) {
      navigate('/ask', { state: { prefill: `${context} for ${fuelType}` } })
    } else {
      navigate('/ask', { state: { prefill: context } })
    }
  }

  const chartData = useMemo(() => {
    if (!data || data.length === 0) return []

    const grouped = data.reduce(
      (acc, row) => {
        const key =
          viewType === 'annual'
            ? String(row.periodYear)
            : `Q${(row as MbieGenerationQuarterlyRecord).periodQuarter} ${row.periodYear}`
        acc[key] = (acc[key] || 0) + row.generationGwh
        return acc
      },
      {} as Record<string, number>
    )

    return Object.entries(grouped)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-10)
      .map(([label, value]) => ({ label, value }))
  }, [data, viewType])

  return (
    <div className="max-w-6xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          MBIE Electricity Generation Data
        </h1>

        <div className="flex flex-wrap gap-4 mb-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              View Type
            </label>
            <select
              value={viewType}
              onChange={e =>
                setViewType(e.target.value as 'annual' | 'quarterly')
              }
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="annual">Annual</option>
              <option value="quarterly">Quarterly</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Fuel Type
            </label>
            <select
              value={fuelType}
              onChange={e => setFuelType(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              disabled={
                viewType === 'annual'
                  ? annualFuelTypes.isLoading
                  : quarterlyFuelTypes.isLoading
              }
            >
              <option value="">All</option>
              {(viewType === 'annual'
                ? annualFuelTypes.data
                : quarterlyFuelTypes.data
              )?.map(fuel => (
                <option key={fuel} value={fuel}>
                  {fuel.charAt(0) + fuel.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
            {(viewType === 'annual'
              ? annualFuelTypes.isLoading
              : quarterlyFuelTypes.isLoading) && (
              <div className="text-sm text-gray-500 mt-1">
                Loading fuel types...
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

        {/* Chart */}
        {!isLoading && chartData.length > 0 && (
          <div className="mb-6">
            <BarChart data={chartData} title="Generation by Period (GWh)" />
          </div>
        )}

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Year
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Fuel Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Generation (GWh)
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td
                    colSpan={3}
                    className="px-6 py-4 text-center text-gray-500"
                  >
                    Loading data...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td
                    colSpan={3}
                    className="px-6 py-4 text-center text-red-500"
                  >
                    Error loading data: {error?.message || 'Unknown error'}
                  </td>
                </tr>
              ) : !data || data.length === 0 ? (
                <tr>
                  <td
                    colSpan={3}
                    className="px-6 py-4 text-center text-gray-500"
                  >
                    No data available
                  </td>
                </tr>
              ) : (
                data.map(
                  (
                    row:
                      | MbieGenerationAnnualRecord
                      | MbieGenerationQuarterlyRecord
                  ) => (
                    <tr key={row.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {viewType === 'annual'
                          ? row.periodYear
                          : `Q${(row as MbieGenerationQuarterlyRecord).periodQuarter} ${row.periodYear}`}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {row.fuelTypeRaw}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {row.generationGwh.toLocaleString()}
                      </td>
                    </tr>
                  )
                )
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

export default MbieBrowsePage
