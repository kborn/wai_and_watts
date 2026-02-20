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

const normalizeFuelType = (value?: string) =>
  value ? value.trim().replace(/\s+/g, ' ').toUpperCase() : ''

const normalizeRawFuelLabel = (value?: string) =>
  value ? value.trim().replace(/\s+/g, ' ') : 'Unknown'

const MbieBrowsePage: React.FC = () => {
  const [viewType, setViewType] = useState<'annual' | 'quarterly'>('annual')
  const [selectedFuels, setSelectedFuels] = useState<string[]>([])
  const [hasTouchedFuelFilter, setHasTouchedFuelFilter] = useState(false)
  const [showTotal, setShowTotal] = useState(false)
  const [activeTab, setActiveTab] = useState<'chart' | 'table'>('chart')
  const [zoomRange, setZoomRange] = useState<[number, number] | null>(null)
  const navigate = useNavigate()

  const handleTabChange = (tab: 'chart' | 'table') => {
    setActiveTab(tab)
  }

  const annualFuelTypes = useMbieGenerationAnnualFuelTypes()
  const quarterlyFuelTypes = useMbieGenerationQuarterlyFuelTypes()

  const fuelTypesLoading =
    viewType === 'annual'
      ? annualFuelTypes.isLoading
      : quarterlyFuelTypes.isLoading

  const allFuelTypes = useMemo(
    () =>
      (viewType === 'annual'
        ? annualFuelTypes.data
        : quarterlyFuelTypes.data
      )?.map(f => f.toUpperCase()) || [],
    [viewType, annualFuelTypes.data, quarterlyFuelTypes.data]
  )

  const normalizedSelectedFuels = useMemo(
    () => selectedFuels.filter(fuel => allFuelTypes.includes(fuel)),
    [selectedFuels, allFuelTypes]
  )

  const effectiveSelectedFuels = useMemo(() => {
    if (!hasTouchedFuelFilter) return allFuelTypes
    return normalizedSelectedFuels
  }, [hasTouchedFuelFilter, allFuelTypes, normalizedSelectedFuels])

  const annualData = useMbieGenerationAnnual({})
  const quarterlyData = useMbieGenerationQuarterly({})

  const annualRecords = annualData.data || []
  const quarterlyRecords = quarterlyData.data || []
  const data = viewType === 'annual' ? annualRecords : quarterlyRecords
  const isLoading =
    viewType === 'annual' ? annualData.isLoading : quarterlyData.isLoading
  const error = viewType === 'annual' ? annualData.error : quarterlyData.error

  const handleFuelToggle = (fuel: string) => {
    if (!allFuelTypes.length) return
    setHasTouchedFuelFilter(true)
    const base = hasTouchedFuelFilter ? normalizedSelectedFuels : allFuelTypes
    setSelectedFuels(
      base.includes(fuel) ? base.filter(f => f !== fuel) : [...base, fuel]
    )
  }

  const handleSelectAll = () => {
    if (allFuelTypes.length === 0) return
    setHasTouchedFuelFilter(true)
    if (effectiveSelectedFuels.length === allFuelTypes.length) {
      setSelectedFuels([])
    } else {
      setSelectedFuels(allFuelTypes)
    }
  }

  const handleExplainThis = () => {
    navigate('/ask')
  }

  const handleZoomChange = (startIndex: number, endIndex: number) => {
    setZoomRange([startIndex, endIndex])
  }

  const handleResetZoom = () => {
    setZoomRange(null)
  }

  const chartData = useMemo(() => {
    if (!data || data.length === 0) return []

    const aggregated = new Map<
      string,
      {
        periodLabel: string
        periodValue: number
        fuelType: string
        generationGwh: number
        breakdown: Record<string, number>
      }
    >()

    data.forEach(row => {
      const periodValue =
        viewType === 'annual'
          ? row.periodYear
          : (row as MbieGenerationQuarterlyRecord).periodQuarter -
            1 +
            row.periodYear * 4

      const periodLabel =
        viewType === 'annual'
          ? String(row.periodYear)
          : `Q${(row as MbieGenerationQuarterlyRecord).periodQuarter} ${row.periodYear}`

      const normalizedFuelType = normalizeFuelType(
        row.fuelTypeNorm || row.fuelType || row.fuelTypeRaw
      )
      const rawLabel = normalizeRawFuelLabel(row.fuelTypeRaw || row.fuelType)
      const key = `${periodValue}-${normalizedFuelType}`

      const existing = aggregated.get(key)
      if (existing) {
        existing.generationGwh += row.generationGwh
        existing.breakdown[rawLabel] =
          (existing.breakdown[rawLabel] || 0) + row.generationGwh
      } else {
        aggregated.set(key, {
          periodLabel,
          periodValue,
          fuelType: normalizedFuelType,
          generationGwh: row.generationGwh,
          breakdown: { [rawLabel]: row.generationGwh },
        })
      }
    })

    return Array.from(aggregated.values()).sort(
      (a, b) => a.periodValue - b.periodValue
    )
  }, [data, viewType])

  const filteredByFuel = useMemo(() => {
    if (effectiveSelectedFuels.length === 0) return []
    return chartData.filter(d => {
      const fuelTypeUpper = d.fuelType?.toUpperCase()
      return effectiveSelectedFuels.includes(fuelTypeUpper || '')
    })
  }, [chartData, effectiveSelectedFuels])

  const allPeriods = useMemo(() => {
    return [...new Set(filteredByFuel.map(d => d.periodValue))].sort(
      (a, b) => a - b
    )
  }, [filteredByFuel])

  const filteredData = useMemo(() => {
    if (!zoomRange) return filteredByFuel

    const startPeriod = allPeriods[zoomRange[0]]
    const endPeriod = allPeriods[zoomRange[1]]

    if (startPeriod === undefined || endPeriod === undefined)
      return filteredByFuel

    return filteredByFuel.filter(
      d => d.periodValue >= startPeriod && d.periodValue <= endPeriod
    )
  }, [filteredByFuel, zoomRange, allPeriods])

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
              onChange={e => {
                setViewType(e.target.value as 'annual' | 'quarterly')
                setZoomRange(null)
                setSelectedFuels([])
                setHasTouchedFuelFilter(false)
              }}
              className="select-base"
            >
              <option value="annual">Annual</option>
              <option value="quarterly">Quarterly</option>
            </select>
          </div>

          <div className="min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Fuel Types
            </label>
            {fuelTypesLoading ? (
              <div className="text-xs text-gray-500 py-2">
                Loading fuel types...
              </div>
            ) : (
              <>
                <div className="text-xs text-gray-500 mb-1">
                  <button
                    onClick={handleSelectAll}
                    className="text-blue-600 hover:underline"
                  >
                    {effectiveSelectedFuels.length === allFuelTypes.length &&
                    allFuelTypes.length > 0
                      ? 'Deselect All'
                      : 'Select All'}
                  </button>
                </div>
                <div className="flex flex-wrap gap-2 max-h-24 overflow-y-auto border rounded p-2">
                  {allFuelTypes.map(fuel => {
                    const isSelected = effectiveSelectedFuels.includes(fuel)
                    return (
                      <label
                        key={fuel}
                        className="flex items-center gap-1 text-xs"
                      >
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleFuelToggle(fuel)}
                          className="rounded"
                        />
                        {fuel}
                      </label>
                    )
                  })}
                </div>
              </>
            )}
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
            onClick={() => handleTabChange('chart')}
            className={`px-4 py-2 text-sm font-medium ${
              activeTab === 'chart'
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Chart
          </button>
          <button
            onClick={() => handleTabChange('table')}
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
          <div>
            {zoomRange && (
              <div className="flex justify-end mb-2">
                <button
                  onClick={handleResetZoom}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  Reset zoom
                </button>
              </div>
            )}
            <MbieTimelineChart
              data={filteredByFuel}
              viewType={viewType}
              showTotal={showTotal}
              zoomRange={zoomRange}
              onZoomChange={handleZoomChange}
            />
          </div>
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
                ) : filteredData.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-6 py-8 text-center">
                      <p className="text-gray-500 text-sm">No data available</p>
                    </td>
                  </tr>
                ) : (
                  filteredData.map(row => (
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
          {isLoading || fuelTypesLoading
            ? 'Loading data from backend...'
            : error
              ? 'Failed to load data from backend.'
              : `Showing ${filteredData.length} of ${filteredByFuel.length} records${zoomRange ? ' (filtered by zoom)' : ''}.`}
        </div>
      </div>
    </div>
  )
}

export default MbieBrowsePage
