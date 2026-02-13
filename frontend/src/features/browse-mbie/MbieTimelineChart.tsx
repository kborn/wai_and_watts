import React, { useMemo, useCallback } from 'react'
import ReactECharts from 'echarts-for-react'

interface DataPoint {
  periodLabel: string
  periodValue: number
  fuelType: string
  generationGwh: number
}

interface MbieTimelineChartProps {
  data: DataPoint[]
  viewType: 'annual' | 'quarterly'
  showTotal?: boolean
  onZoomChange?: (startIndex: number, endIndex: number) => void
}

export const MbieTimelineChart: React.FC<MbieTimelineChartProps> = ({
  data,
  viewType,
  showTotal = false,
  onZoomChange,
}) => {
  const { series, xAxisLabels } = useMemo(() => {
    if (!data || data.length === 0) {
      return { series: [], xAxisLabels: [] }
    }

    const allPeriods = [...new Set(data.map(d => d.periodValue))].sort((a, b) => a - b)
    
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

    const groupedByFuel = data.reduce((acc, row) => {
      const key = row.fuelType
      if (!acc[key]) {
        acc[key] = []
      }
      const index = periodToIndex.get(row.periodValue)!
      acc[key].push({ index, value: row.generationGwh })
      return acc
    }, {} as Record<string, { index: number; value: number }[]>)

    type SeriesItem = { name: string; type: 'line'; data: number[][]; smooth: boolean; symbol: string; symbolSize?: number }

    const seriesList: SeriesItem[] = Object.entries(groupedByFuel).map(([fuelType, points]) => ({
      name: fuelType,
      type: 'line' as const,
      data: points.sort((a, b) => a.index - b.index).map(p => [p.index, p.value]),
      smooth: false,
      symbol: 'circle',
      symbolSize: 6,
    }))

    if (showTotal && seriesList.length > 0) {
      const totalPoints = allPeriods.map((_, i) => {
        const sum = seriesList.reduce((acc, s) => {
          const point = s.data.find(d => d[0] === i)
          return acc + (point ? point[1] : 0)
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

  const handleEvents = useCallback((chart: any) => {
    if (!chart) return
    
    chart.on('datazoom', (_params: any) => {
      const chartOption = chart.getOption()
      const dataZoom = chartOption.dataZoom[0]
      if (dataZoom && onZoomChange) {
        const start = dataZoom.start
        const end = dataZoom.end
        const startIndex = Math.floor((start / 100) * xAxisLabels.length)
        const endIndex = Math.ceil((end / 100) * xAxisLabels.length)
        onZoomChange(startIndex, endIndex)
      }
    })
  }, [onZoomChange, xAxisLabels.length])

  const option = useMemo(() => ({
    tooltip: {
      trigger: 'axis' as const,
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
      },
      {
        type: 'slider' as const,
        bottom: 20,
      },
    ],
    series,
  }), [series, xAxisLabels])

  if (!data || data.length === 0) {
    return null
  }

  return (
    <div className="card p-4">
      <ReactECharts
        option={option}
        style={{ height: '350px', width: '100%' }}
        opts={{ renderer: 'svg' }}
        onEvents={{ datazoom: handleEvents }}
      />
    </div>
  )
}
