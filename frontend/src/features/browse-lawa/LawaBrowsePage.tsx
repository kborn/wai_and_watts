import React, { useState, useMemo, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import ReactECharts from 'echarts-for-react'
import {
  useLawaStateMultiYear,
  useLawaTrendMultiYear,
  useLawaStateRegions,
  useLawaStateIndicators,
  useLawaTrendRegions,
  useLawaTrendIndicators,
} from '../../api/hooks'
import { Card, CardContent, Button } from '../../components/ui'
import RegionContextPanel from './RegionContextPanel'

const createHatchPattern = (): HTMLCanvasElement | undefined => {
  if (typeof document === 'undefined') return undefined
  const canvas = document.createElement('canvas')
  canvas.width = 8
  canvas.height = 8
  const ctx = canvas.getContext('2d')
  if (!ctx) return undefined
  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, 8, 8)
  ctx.strokeStyle = '#d4d4d4'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(0, 8)
  ctx.lineTo(8, 0)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(-2, 10)
  ctx.lineTo(10, -2)
  ctx.stroke()
  return canvas
}

const formatRegionDisplay = (region: string): string => {
  return region
    .trim()
    .split(/\s+/)
    .map(word =>
      word
        .split('-')
        .map(segment =>
          segment
            .split("'")
            .map((part, index) => {
              const lower = part.toLowerCase()
              if (!lower) return lower
              if (index > 0 && (lower === 's' || lower === 't')) {
                return lower
              }
              return `${lower.charAt(0).toUpperCase()}${lower.slice(1)}`
            })
            .join("'")
        )
        .join('-')
    )
    .join(' ')
}

const LawaBrowsePage: React.FC = () => {
  const [viewType, setViewType] = useState<'state' | 'trend'>('state')
  const [region, setRegion] = useState('')
  const [indicator, setIndicator] = useState('')
  const [selectedSite, setSelectedSite] = useState<string | null>(null)
  const [trendClassificationFilter, setTrendClassificationFilter] = useState<
    string | null
  >(null)
  const [stateBandFilter, setStateBandFilter] = useState<string | null>(null)
  const navigate = useNavigate()

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

  const isLoading =
    viewType === 'state' ? stateData.isLoading : trendData.isLoading
  const error = viewType === 'state' ? stateData.error : trendData.error
  const rawData = viewType === 'state' ? stateData.data : trendData.data

  const trendGatingSatisfied = !!region || !!indicator
  const stateGatingSatisfied = !!region || !!indicator

  const canLoadData =
    viewType === 'trend' ? trendGatingSatisfied : stateGatingSatisfied
  const data = canLoadData ? rawData : []

  const handleExplainThis = () => {
    navigate('/ask')
  }

  const normalizeTrendScore = useCallback(
    (score: number | null): number | null => {
      if (score === null || score === undefined) return null
      if (score === -99) return null
      return score
    },
    []
  )

  const getTrendClassification = useCallback(
    (score: number | null): string => {
      const normalizedScore = normalizeTrendScore(score)
      if (normalizedScore === null) return 'Insufficient Data'
      switch (normalizedScore) {
        case -2:
          return 'Very Likely Degrading'
        case -1:
          return 'Likely Degrading'
        case 0:
          return 'Indeterminate'
        case 1:
          return 'Likely Improving'
        case 2:
          return 'Very Likely Improving'
        default:
          return 'Insufficient Data'
      }
    },
    [normalizeTrendScore]
  )

  const TREND_BUCKETS = [
    'Very Likely Degrading',
    'Likely Degrading',
    'Indeterminate',
    'Likely Improving',
    'Very Likely Improving',
    'Insufficient Data',
  ]

  const trendChartData = useMemo(() => {
    if (!data || data.length === 0 || viewType !== 'trend') return []

    const bucketCounts = TREND_BUCKETS.reduce(
      (acc, bucket) => ({ ...acc, [bucket]: 0 }),
      {} as Record<string, number>
    )

    data.forEach(row => {
      const score = 'trendScore' in row ? row.trendScore : null
      const classification = getTrendClassification(score)
      if (bucketCounts[classification] !== undefined) {
        bucketCounts[classification]++
      }
    })

    const total = Object.values(bucketCounts).reduce((a, b) => a + b, 0)

    return TREND_BUCKETS.filter(bucket => bucketCounts[bucket] > 0).map(
      bucket => ({
        name: bucket,
        value: bucketCounts[bucket],
        percent: total > 0 ? (bucketCounts[bucket] / total) * 100 : 0,
      })
    )
  }, [data, viewType, getTrendClassification])

  const trendTotalSites = trendChartData.reduce((sum, d) => sum + d.value, 0)

  const filteredTrendData = useMemo(() => {
    if (!data || viewType !== 'trend') return []
    if (!trendClassificationFilter) return data

    return data.filter(row => {
      const score = 'trendScore' in row ? row.trendScore : null
      const classification = getTrendClassification(score)
      return classification === trendClassificationFilter
    })
  }, [data, viewType, trendClassificationFilter, getTrendClassification])

  const filteredStateData = useMemo(() => {
    if (!data || viewType !== 'state') return []
    if (!stateBandFilter) return data

    return data.filter(row => {
      if ('attributeBand' in row) {
        const median = 'median' in row ? row.median : null
        if (stateBandFilter === 'Insufficient Data') {
          return median === null || median === -99
        }
        return row.attributeBand === stateBandFilter
      }
      return false
    })
  }, [data, viewType, stateBandFilter])

  const displayData =
    viewType === 'trend' ? filteredTrendData : filteredStateData

  const trendChartOption = useMemo(() => {
    const total = trendChartData.reduce((sum, d) => sum + d.value, 0)
    return {
      tooltip: {
        trigger: 'item' as const,
        formatter: (params: {
          name?: string
          value?: number
          data?: { percent?: number }
        }) => {
          const name = params.name || ''
          const val = params.value ?? 0
          const percent =
            params.data?.percent ?? (total > 0 ? (val / total) * 100 : 0)
          return `${name}<br/>${val} monitoring sites<br/>${percent.toFixed(1)}% of monitored sites`
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category' as const,
        data: trendChartData.map(d => d.name),
        axisLabel: {
          rotate: 45,
          fontSize: 10,
        },
      },
      yAxis: {
        type: 'value' as const,
        name: 'Number of Sites',
        nameTextStyle: {
          fontSize: 11,
          padding: [0, 0, 0, 40],
        },
      },
      series: [
        {
          data: trendChartData.map((d, i) => ({
            value: d.value,
            percent: d.percent,
            itemStyle:
              d.name === 'Insufficient Data'
                ? {
                    color: {
                      image: createHatchPattern(),
                      repeat: 'repeat',
                    },
                  }
                : {
                    color: [
                      '#dc2626',
                      '#f97316',
                      '#a3a3a3',
                      '#22c55e',
                      '#16a34a',
                    ][i],
                  },
          })),
          type: 'bar' as const,
          label: {
            show: true,
            position: 'top' as const,
            formatter: '{c}',
            fontSize: 11,
            fontWeight: 'bold' as const,
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
    }
  }, [trendChartData])

  const handleTrendBarClick = useCallback(
    (params: unknown) => {
      if (viewType !== 'trend') return
      const p = params as { name?: string } | undefined
      if (!p?.name) return
      const clickedBucket = p.name
      setTrendClassificationFilter(prev =>
        prev === clickedBucket ? null : clickedBucket
      )
    },
    [viewType]
  )

  const BAND_ORDER = ['A', 'B', 'C', 'D', 'E']

  const stateBandChartData = useMemo(() => {
    if (!data || viewType !== 'state' || (!region && !indicator)) return []

    const bandCounts: Record<string, number> = {}
    let insufficientCount = 0

    data.forEach(row => {
      if ('attributeBand' in row) {
        const band = row.attributeBand
        const median = 'median' in row ? row.median : null

        if (median === null || median === -99) {
          insufficientCount++
        } else if (band) {
          bandCounts[band] = (bandCounts[band] || 0) + 1
        }
      }
    })

    const total =
      Object.values(bandCounts).reduce((a, b) => a + b, 0) + insufficientCount

    const result: {
      name: string
      value: number
      percent: number
      isInsufficient: boolean
    }[] = []

    BAND_ORDER.forEach(band => {
      if (bandCounts[band] !== undefined) {
        result.push({
          name: band,
          value: bandCounts[band],
          percent: total > 0 ? (bandCounts[band] / total) * 100 : 0,
          isInsufficient: false,
        })
      }
    })

    if (insufficientCount > 0) {
      result.push({
        name: 'Insufficient Data',
        value: insufficientCount,
        percent: total > 0 ? (insufficientCount / total) * 100 : 0,
        isInsufficient: true,
      })
    }

    return result
  }, [data, viewType, region, indicator])

  const stateTotalSites = stateBandChartData.reduce(
    (sum, d) => sum + d.value,
    0
  )

  const stateChartOption = useMemo(() => {
    if (stateBandChartData.length === 0) return null

    const total = stateBandChartData.reduce((sum, d) => sum + d.value, 0)

    return {
      tooltip: {
        trigger: 'item' as const,
        formatter: (params: {
          name?: string
          value?: number
          data?: { percent?: number; isInsufficient?: boolean }
        }) => {
          const name = params.name || ''
          const val = params.value ?? 0
          const percent =
            params.data?.percent ?? (total > 0 ? (val / total) * 100 : 0)
          return `${name}<br/>${val} monitoring sites<br/>${percent.toFixed(1)}% of total`
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category' as const,
        data: stateBandChartData.map(d => d.name),
        axisLabel: {
          fontSize: 12,
          fontWeight: 'bold',
        },
      },
      yAxis: {
        type: 'value' as const,
        name: 'Number of Sites',
        nameTextStyle: {
          fontSize: 11,
          padding: [0, 0, 0, 40],
        },
      },
      series: [
        {
          data: stateBandChartData.map(d => ({
            value: d.value,
            percent: d.percent,
            isInsufficient: d.isInsufficient,
            itemStyle: d.isInsufficient
              ? {
                  color: {
                    image: createHatchPattern(),
                    repeat: 'repeat',
                  },
                }
              : {
                  color:
                    d.name === 'A'
                      ? '#16a34a'
                      : d.name === 'B'
                        ? '#22c55e'
                        : d.name === 'C'
                          ? '#eab308'
                          : d.name === 'D'
                            ? '#f97316'
                            : d.name === 'E'
                              ? '#dc2626'
                              : '#a3a3a3',
                },
          })),
          type: 'bar' as const,
          label: {
            show: true,
            position: 'top' as const,
            formatter: '{c}',
            fontSize: 11,
            fontWeight: 'bold' as const,
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
    }
  }, [stateBandChartData])

  const handleStateBandClick = useCallback(
    (params: unknown) => {
      if (viewType !== 'state') return
      const p = params as { name?: string } | undefined
      if (!p?.name) return
      const clickedBand = p.name
      setStateBandFilter(prev => (prev === clickedBand ? null : clickedBand))
    },
    [viewType]
  )

  const handleStateRowClick = (row: {
    lawaSiteId: string
    siteName: string
  }) => {
    setSelectedSite(row.lawaSiteId)
  }

  const getStateBadgeClass = (stateNorm: string) => {
    switch (stateNorm) {
      case 'EXCELLENT':
        return 'bg-green-100 text-green-800'
      case 'GOOD':
        return 'bg-blue-100 text-blue-800'
      case 'FAIR':
        return 'bg-yellow-100 text-yellow-800'
      case 'POOR':
        return 'bg-red-100 text-red-800'
      case 'VERY_POOR':
        return 'bg-red-200 text-red-900'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getTrendBadgeClass = (classification: string) => {
    switch (classification) {
      case 'Very Likely Improving':
        return 'bg-green-200 text-green-900'
      case 'Likely Improving':
        return 'bg-green-100 text-green-800'
      case 'Very Likely Degrading':
        return 'bg-red-200 text-red-900'
      case 'Likely Degrading':
        return 'bg-red-100 text-red-800'
      case 'Indeterminate':
        return 'bg-neutral-200 text-neutral-800'
      case 'Insufficient Data':
        return 'bg-neutral-100 text-neutral-600'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getEmptyStateMessage = () => {
    if (!region && !indicator) {
      return 'Select a Region or Indicator to view data'
    }
    return 'No data available for the selected filters'
  }

  return (
    <div className="section-container">
      <div className="text-center mb-8">
        <h1 className="text-h2 font-semibold text-neutral-900 mb-3">
          LAWA Water Quality
        </h1>
        <p className="text-body text-neutral-600">
          Explore New Zealand river monitoring site state and trend data by
          region and indicator.
        </p>
      </div>

      <Card className="mb-6">
        <CardContent>
          <div className="flex flex-wrap gap-4 items-end">
            <div className="min-w-[160px]">
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Region
              </label>
              <select
                value={region}
                onChange={e => {
                  setRegion(e.target.value)
                  setSelectedSite(null)
                  setStateBandFilter(null)
                }}
                className="select-base"
                disabled={
                  viewType === 'state'
                    ? stateRegions.isLoading
                    : trendRegions.isLoading
                }
              >
                <option value="">All Regions</option>
                {(viewType === 'state'
                  ? stateRegions.data
                  : trendRegions.data
                )?.map(r => (
                  <option key={r} value={r}>
                    {formatRegionDisplay(r)}
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
                onChange={e => {
                  setIndicator(e.target.value)
                  setSelectedSite(null)
                  setStateBandFilter(null)
                }}
                className="select-base"
                disabled={
                  viewType === 'state'
                    ? stateIndicators.isLoading
                    : trendIndicators.isLoading
                }
              >
                <option value="">All Indicators</option>
                {(viewType === 'state'
                  ? stateIndicators.data
                  : trendIndicators.data
                )?.map(ind => (
                  <option key={ind} value={ind}>
                    {ind}
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
              <div className="text-xs text-neutral-500 mt-1">
                Indicator codes are dataset-specific and differ between State
                and Trend views.
              </div>
            </div>

            <Button onClick={handleExplainThis}>Explain This Data</Button>
          </div>

          {viewType === 'trend' && trendClassificationFilter && (
            <div className="mt-3 flex items-center gap-2">
              <span className="text-sm text-neutral-600">
                Filtering by:{' '}
                <span className="font-medium">{trendClassificationFilter}</span>
              </span>
              <button
                onClick={() => setTrendClassificationFilter(null)}
                className="text-sm text-primary-600 hover:text-primary-800"
              >
                Clear filter
              </button>
            </div>
          )}
        </CardContent>
      </Card>

      {region && (
        <RegionContextPanel
          region={region}
          indicator={indicator || undefined}
        />
      )}

      <Card className="mb-4">
        <CardContent className="py-3">
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-neutral-700">View:</span>
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setViewType('state')
                  setIndicator('')
                  setSelectedSite(null)
                  setTrendClassificationFilter(null)
                  setStateBandFilter(null)
                }}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  viewType === 'state'
                    ? 'bg-primary-600 text-white'
                    : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
                }`}
              >
                State
              </button>
              <button
                onClick={() => {
                  setViewType('trend')
                  setIndicator('')
                  setSelectedSite(null)
                  setTrendClassificationFilter(null)
                  setStateBandFilter(null)
                }}
                className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  viewType === 'trend'
                    ? 'bg-primary-600 text-white'
                    : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
                }`}
              >
                Trend
              </button>
            </div>
          </div>
        </CardContent>
      </Card>

      {!canLoadData ? (
        <Card>
          <CardContent>
            <div className="text-center py-8">
              <p className="text-neutral-600">{getEmptyStateMessage()}</p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <>
          {viewType === 'state' &&
            (region || indicator) &&
            stateBandChartData.length > 0 && (
              <Card className="mb-6">
                <CardContent>
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="text-sm font-medium text-neutral-700">
                        State Band Distribution
                      </h3>
                      <p className="text-xs text-neutral-500 mt-1">
                        {region && `Region: ${formatRegionDisplay(region)}`}
                        {region && indicator && ' | '}
                        {indicator && `Indicator: ${indicator}`}
                      </p>
                    </div>
                    <span className="text-sm text-neutral-600 bg-neutral-100 px-3 py-1 rounded-full">
                      {stateTotalSites} monitoring sites total
                    </span>
                  </div>
                  {stateBandFilter && (
                    <p className="text-sm text-neutral-500 mb-3">
                      (Filtered by:{' '}
                      <span className="font-medium">{stateBandFilter}</span>)
                    </p>
                  )}
                  <ReactECharts
                    option={stateChartOption}
                    style={{ height: '300px', width: '100%' }}
                    opts={{ renderer: 'svg' }}
                    onEvents={{ click: handleStateBandClick }}
                  />
                  <p className="text-xs text-neutral-500 mt-2">
                    Click a bar to filter the table by that band
                  </p>
                </CardContent>
              </Card>
            )}

          {viewType === 'state' && (!region || !indicator) && (
            <Card className="mb-6">
              <CardContent>
                <p className="text-sm text-neutral-500 py-4 text-center">
                  Select Region or Indicator to view State band distribution.
                </p>
              </CardContent>
            </Card>
          )}

          {viewType === 'trend' && trendChartData.length > 0 && (
            <Card className="mb-6">
              <CardContent>
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-sm font-medium text-neutral-700">
                      Trend Classification Distribution
                    </h3>
                    <p className="text-xs text-neutral-500 mt-1">
                      {region && `Region: ${formatRegionDisplay(region)}`}
                      {region && indicator && ' | '}
                      {indicator && `Indicator: ${indicator}`}
                    </p>
                  </div>
                  <span className="text-sm text-neutral-600 bg-neutral-100 px-3 py-1 rounded-full">
                    {trendTotalSites} monitoring sites total
                  </span>
                </div>
                {trendClassificationFilter && (
                  <p className="text-sm text-neutral-500 mb-3">
                    (Filtered by:{' '}
                    <span className="font-medium">
                      {trendClassificationFilter}
                    </span>
                    )
                  </p>
                )}
                <ReactECharts
                  option={trendChartOption}
                  style={{ height: '300px', width: '100%' }}
                  opts={{ renderer: 'svg' }}
                  onEvents={{ click: handleTrendBarClick }}
                />
                <p className="text-xs text-neutral-500 mt-2">
                  Click a bar to filter the table by that classification
                </p>
                <p className="text-xs text-neutral-400 mt-1 italic">
                  Trend classifications are based on multi-year statistical
                  windows, not continuous time measurements.
                </p>
              </CardContent>
            </Card>
          )}

          <p className="text-xs text-neutral-500 mb-2">
            ← Scroll horizontally to see more columns →
          </p>

          <div className="table-container overflow-x-auto">
            <table className="table-base">
              <thead>
                <tr className="table-header">
                  <th className="px-4 py-3">Period</th>
                  <th className="px-4 py-3">Region</th>
                  <th className="px-4 py-3">Catchment</th>
                  <th className="px-4 py-3">Site</th>
                  <th className="px-4 py-3">Site ID</th>
                  <th className="px-4 py-3">Indicator</th>
                  {viewType === 'state' ? (
                    <>
                      <th className="px-4 py-3">Attribute Band</th>
                      <th className="px-4 py-3">State</th>
                      <th className="px-4 py-3">Median</th>
                      <th className="px-4 py-3">Units</th>
                      {stateData.data?.some(
                        r => 'p95' in r && r.p95 !== null && r.p95 !== -99
                      ) && <th className="px-4 py-3">95th</th>}
                    </>
                  ) : (
                    <>
                      <th className="px-4 py-3">Trend</th>
                      <th className="px-4 py-3">Score</th>
                      <th className="px-4 py-3">Data Frequency</th>
                    </>
                  )}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={11} className="px-6 py-8 text-center">
                      <div className="spinner w-6 h-6 mx-auto mb-2"></div>
                      <p className="text-neutral-500 text-sm">
                        Loading data...
                      </p>
                    </td>
                  </tr>
                ) : error ? (
                  <tr>
                    <td colSpan={11} className="px-6 py-8 text-center">
                      <p className="text-error-600 text-sm">
                        Error loading data: {error?.message || 'Unknown error'}
                      </p>
                    </td>
                  </tr>
                ) : !displayData || displayData.length === 0 ? (
                  <tr>
                    <td colSpan={11} className="px-6 py-8 text-center">
                      <p className="text-neutral-500 text-sm">
                        No data available
                      </p>
                    </td>
                  </tr>
                ) : (
                  displayData.map(row => {
                    const isSelected =
                      viewType === 'state' &&
                      'lawaSiteId' in row &&
                      selectedSite === row.lawaSiteId
                    return (
                      <tr
                        key={row.id}
                        className={`table-row ${viewType === 'state' ? 'cursor-pointer hover:bg-primary-50' : ''} ${isSelected ? 'bg-primary-50' : ''}`}
                        onClick={() =>
                          viewType === 'state' &&
                          'lawaSiteId' in row &&
                          handleStateRowClick(row)
                        }
                      >
                        <td className="table-cell font-medium">
                          {row.periodStartYear}-{row.periodEndYear}
                        </td>
                        <td className="table-cell">
                          {formatRegionDisplay(row.region)}
                        </td>
                        <td className="table-cell">
                          {row.catchment
                            ? formatRegionDisplay(row.catchment)
                            : '-'}
                        </td>
                        <td className="table-cell">{row.siteName}</td>
                        <td className="table-cell text-neutral-500">
                          {row.lawaSiteId}
                        </td>
                        <td className="table-cell">{row.indicatorRaw}</td>
                        {viewType === 'state' ? (
                          <>
                            <td className="table-cell">
                              {'attributeBand' in row ? row.attributeBand : '-'}
                            </td>
                            <td className="table-cell">
                              {'stateNorm' in row && (
                                <span
                                  className={`px-2 py-1 text-xs rounded-full ${getStateBadgeClass(row.stateNorm)}`}
                                >
                                  {row.stateNorm}
                                </span>
                              )}
                            </td>
                            <td className="table-cell">
                              {'median' in row
                                ? row.median === null || row.median === -99
                                  ? 'Insufficient Data'
                                  : row.median.toLocaleString()
                                : '-'}
                            </td>
                            <td className="table-cell">{row.units || '-'}</td>
                            {'p95' in row &&
                              row.p95 !== null &&
                              row.p95 !== -99 && (
                                <td className="table-cell">
                                  {row.p95.toLocaleString()}
                                </td>
                              )}
                          </>
                        ) : (
                          <>
                            <td className="table-cell">
                              {'trendScore' in row && (
                                <span
                                  className={`px-2 py-1 text-xs rounded-full ${getTrendBadgeClass(getTrendClassification(row.trendScore))}`}
                                >
                                  {getTrendClassification(row.trendScore)}
                                </span>
                              )}
                            </td>
                            <td className="table-cell">
                              {'trendScore' in row &&
                                (normalizeTrendScore(row.trendScore) === null
                                  ? 'Insufficient Data'
                                  : row.trendScore)}
                            </td>
                            <td className="table-cell">
                              {'trendDataFrequency' in row
                                ? row.trendDataFrequency || '-'
                                : '-'}
                            </td>
                          </>
                        )}
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-4 text-sm text-neutral-600 text-right">
            {isLoading
              ? 'Loading data from backend...'
              : error
                ? 'Failed to load data from backend.'
                : `Showing ${displayData?.length || 0} records.`}
          </div>
        </>
      )}
    </div>
  )
}

export default LawaBrowsePage
