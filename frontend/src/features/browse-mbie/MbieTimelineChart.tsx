import React, { useMemo, useCallback } from 'react'
import ReactECharts from 'echarts-for-react'

interface DataPoint {
  periodLabel: string
  periodValue: number
  fuelType: string
  generationGwh: number
  breakdown?: Record<string, number>
}

interface MbieTimelineChartProps {
  data: DataPoint[]
  viewType: 'annual' | 'quarterly'
  showTotal?: boolean
  zoomRange?: [number, number] | null
  onZoomChange?: (startIndex: number, endIndex: number) => void
}

type DataZoomState = { start?: number; end?: number }
type ChartOptionLike = { dataZoom?: DataZoomState[] }
type EChartsLike = { getOption: () => ChartOptionLike }

export const MbieTimelineChart: React.FC<MbieTimelineChartProps> = ({
  data,
  viewType,
  showTotal = false,
  zoomRange = null,
  onZoomChange,
}) => {
  const { series, xAxisLabels } = useMemo(() => {
    if (!data || data.length === 0) {
      return { series: [], xAxisLabels: [] }
    }

    const allPeriods = [...new Set(data.map(d => d.periodValue))].sort(
      (a, b) => a - b
    )

    const xAxisLabels = allPeriods.map(p => {
      if (viewType === 'annual') {
        return String(p)
      } else {
        const year = Math.floor(p / 4)
        const q = (p % 4) + 1
        return `Q${q} ${year}`
      }
    })

    const periodToIndex = new Map<number, number>()
    allPeriods.forEach((p, i) => periodToIndex.set(p, i))

    const groupedByFuel = data.reduce(
      (acc, row) => {
        const key = row.fuelType
        if (!acc[key]) {
          acc[key] = []
        }
        const index = periodToIndex.get(row.periodValue)!
        acc[key].push({
          index,
          value: row.generationGwh,
          breakdown: row.breakdown,
        })
        return acc
      },
      {} as Record<
        string,
        { index: number; value: number; breakdown?: Record<string, number> }[]
      >
    )

    type SeriesItem = {
      name: string
      type: 'line'
      data: Array<
        | number[]
        | {
            value: number[]
            breakdown?: Record<string, number>
          }
      >
      smooth: boolean
      symbol: string
      symbolSize?: number
    }

    const seriesList: SeriesItem[] = Object.entries(groupedByFuel).map(
      ([fuelType, points]) => ({
        name: fuelType,
        type: 'line' as const,
        data: points
          .sort((a, b) => a.index - b.index)
          .map(p => ({
            value: [p.index, p.value],
            breakdown: p.breakdown,
          })),
        smooth: false,
        symbol: 'circle',
        symbolSize: 6,
      })
    )

    if (showTotal && seriesList.length > 0) {
      const totalPoints = allPeriods.map((_, i) => {
        const sum = seriesList.reduce((acc, s) => {
          const point = s.data.find(d =>
            Array.isArray(d) ? d[0] === i : d.value?.[0] === i
          )
          const value = Array.isArray(point) ? point[1] : point?.value?.[1]
          return acc + (value ?? 0)
        }, 0)
        return [i, sum]
      })
      seriesList.push({
        name: 'Total (sum of displayed fuels)',
        type: 'line' as const,
        data: totalPoints,
        smooth: false,
        symbol: 'none',
      })
    }

    return { series: seriesList, xAxisLabels }
  }, [data, viewType, showTotal])

  const handleDataZoom = useCallback(
    (_params: unknown, chart: EChartsLike) => {
      if (!chart || !onZoomChange) return

      const chartOption = chart.getOption()
      const dataZooms = chartOption.dataZoom || []
      const dataZoom =
        dataZooms.find(dz => typeof dz.start === 'number') || dataZooms[0]
      if (!dataZoom || xAxisLabels.length === 0) return

      const start = typeof dataZoom.start === 'number' ? dataZoom.start : 0
      const end = typeof dataZoom.end === 'number' ? dataZoom.end : 100

      const length = xAxisLabels.length
      const startIndex = Math.max(
        0,
        Math.min(length - 1, Math.floor((start / 100) * length))
      )
      const endIndex = Math.max(
        0,
        Math.min(length - 1, Math.ceil((end / 100) * length) - 1)
      )

      onZoomChange(startIndex, endIndex)
    },
    [onZoomChange, xAxisLabels.length]
  )

  type TooltipParam = {
    axisValueLabel?: string
    seriesName?: string
    marker?: string
    value?: number[] | number
    data?: { breakdown?: Record<string, number> }
  }

  const formatTooltip = useCallback((params: TooltipParam[] | TooltipParam) => {
    const items = Array.isArray(params) ? params : [params]
    if (items.length === 0) return ''
    const axisLabel = items[0].axisValueLabel || ''
    const lines = [axisLabel]

    items.forEach(item => {
      const valueArray = Array.isArray(item.value) ? item.value : []
      const value =
        valueArray.length > 1
          ? valueArray[1]
          : typeof item.value === 'number'
            ? item.value
            : undefined

      if (value === undefined || !item.seriesName) return

      lines.push(
        `${item.marker || ''}${item.seriesName}: ${value.toLocaleString()}`
      )

      const breakdown = item.data?.breakdown
      if (breakdown) {
        Object.entries(breakdown).forEach(([label, amount]) => {
          lines.push(`&nbsp;&nbsp;${label}: ${amount.toLocaleString()}`)
        })
      }
    })

    return lines.join('<br/>')
  }, [])

  const option = useMemo(
    () => ({
      tooltip: {
        trigger: 'axis' as const,
        formatter: formatTooltip,
      },
      legend: {
        data: series.map(s => s.name),
        bottom: 0,
      },
      grid: {
        left: 80,
        right: 40,
        top: 40,
        bottom: 80,
      },
      xAxis: {
        type: 'category' as const,
        data: xAxisLabels,
        axisLabel: {
          rotate: xAxisLabels.length > 10 ? 45 : 0,
          fontSize: 10,
        },
      },
      yAxis: {
        type: 'value' as const,
        name: 'Generation (GWh)',
        nameTextStyle: {
          fontSize: 11,
          padding: [0, 0, 0, 40],
        },
        axisLabel: {
          fontSize: 10,
          formatter: (value: number) => value.toLocaleString(),
        },
      },
      dataZoom: [
        {
          type: 'inside' as const,
          start:
            zoomRange && xAxisLabels.length > 0
              ? (zoomRange[0] / xAxisLabels.length) * 100
              : undefined,
          end:
            zoomRange && xAxisLabels.length > 0
              ? ((zoomRange[1] + 1) / xAxisLabels.length) * 100
              : undefined,
          startValue: zoomRange ? zoomRange[0] : undefined,
          endValue: zoomRange ? zoomRange[1] : undefined,
        },
        {
          type: 'slider' as const,
          bottom: 20,
          start:
            zoomRange && xAxisLabels.length > 0
              ? (zoomRange[0] / xAxisLabels.length) * 100
              : undefined,
          end:
            zoomRange && xAxisLabels.length > 0
              ? ((zoomRange[1] + 1) / xAxisLabels.length) * 100
              : undefined,
          startValue: zoomRange ? zoomRange[0] : undefined,
          endValue: zoomRange ? zoomRange[1] : undefined,
        },
      ],
      series,
    }),
    [series, xAxisLabels, zoomRange, formatTooltip]
  )

  return (
    <div className="card p-4">
      <ReactECharts
        option={option}
        style={{ height: '350px', width: '100%' }}
        opts={{ renderer: 'svg' }}
        notMerge
        onEvents={{ datazoom: handleDataZoom }}
      />
    </div>
  )
}
