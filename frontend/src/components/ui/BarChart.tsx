import React from 'react'

interface ChartProps {
  data: { label: string; value: number }[]
  title?: string
  height?: number
}

export const BarChart: React.FC<ChartProps> = ({
  data,
  title,
  height = 200,
}) => {
  if (!data || data.length === 0) {
    return null
  }

  const maxValue = Math.max(...data.map(d => d.value))
  const barWidth = Math.max(20, Math.min(60, 400 / data.length))
  const chartWidth = data.length * barWidth + 60
  const chartHeight = height

  return (
    <div className="card p-4">
      {title && (
        <h3 className="text-sm font-medium text-neutral-700 mb-4">{title}</h3>
      )}
      <div className="overflow-x-auto">
        <svg width={chartWidth} height={chartHeight} className="mx-auto">
          {data.map((item, index) => {
            const barHeight = (item.value / maxValue) * (chartHeight - 40)
            const x = index * barWidth + 30
            const y = chartHeight - 20 - barHeight

            return (
              <g key={item.label}>
                <rect
                  x={x}
                  y={y}
                  width={barWidth - 8}
                  height={barHeight}
                  className="fill-primary-500 rounded-t"
                  rx={2}
                />
                <text
                  x={x + (barWidth - 8) / 2}
                  y={chartHeight - 5}
                  textAnchor="middle"
                  className="text-[10px] fill-neutral-600"
                >
                  {item.label}
                </text>
                <text
                  x={x + (barWidth - 8) / 2}
                  y={y - 5}
                  textAnchor="middle"
                  className="text-[10px] fill-neutral-500"
                >
                  {item.value.toLocaleString()}
                </text>
              </g>
            )
          })}
        </svg>
      </div>
    </div>
  )
}
