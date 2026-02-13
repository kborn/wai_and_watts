import React, { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useMbieGenerationAnnual,
  useMbieGenerationQuarterly,
  useMbieGenerationAnnualFuelTypes,
  useMbieGenerationQuarterlyFuelTypes,
} from '../../api/hooks'
import type { MbieGenerationQuarterlyRecord } from '../../types'
import { MbieTimelineChart } from './MbieTimelineChart'

const MbieBrowsePage: React.FC = () => {
  const [viewType, setViewType] = useState<'annual' | 'quarterly'>('annual')
  const [fuelType, setFuelType] = useState('')
  const [showTotal, setShowTotal] = useState(false)
  const [activeTab, setActiveTab] = useState<'chart' | 'table'>('chart')
  const navigate = useNavigate()

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

    return data
      .map(row => {
        const periodValue =
          viewType === 'annual'
            ? row.periodYear
            : (row as MbieGenerationQuarterlyRecord).periodQuarter -
              1 +
              row.periodYear * 4

        return {
          periodLabel:
            viewType === 'annual'
              ? String(row.periodYear)
              : `Q${(row as MbieGenerationQuarterlyRecord).periodQuarter} ${row.periodYear}`,
          periodValue,
          fuelType: row.fuelTypeRaw,
          generationGwh: row.generationGwh,
        }
      })
      .sort((a, b) => a.periodValue - b.periodValue)
  }, [data, viewType])

  const fuelTypes =
    viewType === 'annual' ? annualFuelTypes.data : quarterlyFuelTypes.data

  return (
    <div className="section-container">
      <div className="text-center mb-6">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">
          MBIE Electricity Generation
        </h1>
        <p className="text-gray-600">
          Explore New Zealand's electricity generation data by year and fuel
          type.
        </p>
      </div>

      <div className="card p-4">
        <div className="flex flex-wrap gap-4 items-end mb-4">
          <div className="min-w-[140px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              View Type
            </label>
            <select
              value={viewType}
              onChange={e =>
                setViewType(e.target.value as 'annual' | 'quarterly')
              }
              className="select-base"
            >
              <option value="annual">Annual</option>
              <option value="quarterly">Quarterly</option>
            </select>
          </div>

          <div className="min-w-[160px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Fuel Type
            </label>
            <select
              value={fuelType}
              onChange={e => setFuelType(e.target.value)}
              className="select-base"
              disabled={
                viewType === 'annual'
                  ? annualFuelTypes.isLoading
                  : quarterlyFuelTypes.isLoading
              }
            >
              <option value="">All</option>
              {fuelTypes?.map(fuel => (
                <option key={fuel} value={fuel}>
                  {fuel.charAt(0) + fuel.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={showTotal}
              onChange={e => setShowTotal(e.target.checked)}
              className="rounded"
            />
            Show Total
          </label>

          <button onClick={handleExplainThis} className="btn-primary">
            Explain This Data
          </button>
        </div>

        <div className="flex gap-2 mb-4 border-b">
          <button
            onClick={() => setActiveTab('chart')}
            className={`px-4 py-2 text-sm font-medium ${
              activeTab === 'chart'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Chart
          </button>
          <button
            onClick={() => setActiveTab('table')}
            className={`px-4 py-2 text-sm font-medium ${
              activeTab === 'table'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Table
          </button>
        </div>

        {activeTab === 'chart' && (
          <MbieTimelineChart
            data={chartData}
            viewType={viewType}
            showTotal={showTotal}
          />
        )}

        {activeTab === 'table' && (
          <div className="table-container overflow-x-auto">
            <table className="table-base">
              <thead>
                <tr className="table-header">
                  <th className="px-6 py-3">Year</th>
                  <th className="px-6 py-3">Fuel Type</th>
                  <th className="px-6 py-3">Generation (GWh)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={3} className="px-6 py-8 text-center">
                      <div className="spinner w-6 h-6 mx-auto mb-2"></div>
                      <p className="text-gray-500 text-sm">Loading data...</p>
                    </td>
                  </tr>
                ) : error ? (
                  <tr>
                    <td colSpan={3} className="px-6 py-8 text-center">
                      <p className="text-red-600 text-sm">
                        Error loading data: {error?.message || 'Unknown error'}
                      </p>
                    </td>
                  </tr>
                ) : chartData.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-6 py-8 text-center">
                      <p className="text-gray-500 text-sm">No data available</p>
                    </td>
                  </tr>
                ) : (
                  chartData.map(row => (
                    <tr
                      key={`${row.periodValue}-${row.fuelType}`}
                      className="hover:bg-gray-50"
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {row.periodLabel}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {row.fuelType}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {row.generationGwh.toLocaleString()}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-4 text-sm text-gray-600 text-right">
          {isLoading
            ? 'Loading data from backend...'
            : error
              ? 'Failed to load data from backend.'
              : `Showing ${chartData.length} records.`}
        </div>
      </div>
    </div>
  )
}

export default MbieBrowsePage
