import React, { useMemo } from 'react'
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
}

export const MbieTimelineChart: React.FC<MbieTimelineChartProps> = ({
  data,
  viewType,
  showTotal = false,
}) => {
  const { series, xAxisLabels } = useMemo(() => {
    if (!data || data.length === 0) {
      return { series: [], xAxisLabels: [] }
    }

    const groupedByFuel = data.reduce(
      (acc, row) => {
        const key = row.fuelType
        if (!acc[key]) {
          acc[key] = []
        }
        acc[key].push({ x: row.periodValue, y: row.generationGwh })
        return acc
      },
      {} as Record<string, { x: number; y: number }[]>
    )

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

    type SeriesItem = {
      name: string
      type: 'line'
      data: number[][]
      smooth: boolean
      symbol: string
      symbolSize?: number
    }

    const seriesList: SeriesItem[] = Object.entries(groupedByFuel).map(
      ([fuelType, points]) => ({
        name: fuelType,
        type: 'line' as const,
        data: points.sort((a, b) => a.x - b.x).map(p => [p.x, p.y]),
        smooth: false,
        symbol: 'circle',
        symbolSize: 6,
      })
    )

    if (showTotal && seriesList.length > 0) {
      const totalPoints = allPeriods.map(xValue => {
        const sum = seriesList.reduce((acc, s) => {
          const point = s.data.find(d => d[0] === xValue)
          return acc + (point ? point[1] : 0)
        }, 0)
        return [xValue, sum]
      })
      seriesList.push({
        name: 'Total (sum of displayed fuels)',
        type: 'line' as const,
        data: totalPoints,
        smooth: false,
        symbol: 'none' as const,
      })
    }

    return { series: seriesList, xAxisLabels }
  }, [data, viewType, showTotal])

  const option = useMemo(
    () => ({
      tooltip: {
        trigger: 'axis' as const,
      },
      legend: {
        data: series.map(s => s.name),
        bottom: 0,
      },
      grid: {
        left: 60,
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
    }),
    [series, xAxisLabels]
  )

  if (!data || data.length === 0) {
    return null
  }

  return (
    <div className="card p-4">
      <ReactECharts
        option={option}
        style={{ height: '350px', width: '100%' }}
        opts={{ renderer: 'svg' }}
      />
    </div>
  )
}
