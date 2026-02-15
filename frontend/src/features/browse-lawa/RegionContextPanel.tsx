import React, { useState } from 'react'
import { Card, CardContent, CardHeader } from '../../components/ui'
import { useRegionContext } from '../../api/hooks'
import type { RegionContextFactPack } from '../../types'

interface RegionContextPanelProps {
  region: string
  indicator?: string
}

const DISCLAIMER_TEXT =
  'These signals are presented for situational context only. No causal or statistical relationship between datasets is implied.'

const LoadingSkeleton: React.FC = () => (
  <div className="animate-pulse space-y-2">
    <div className="h-4 bg-neutral-200 rounded w-3/4"></div>
    <div className="h-4 bg-neutral-200 rounded w-1/2"></div>
  </div>
)

const ErrorDisplay: React.FC<{ error: Error }> = ({ error }) => (
  <div className="text-error-600 text-sm p-3 bg-error-50 rounded">
    Failed to load context: {error.message}
  </div>
)

const EmptyDisplay: React.FC = () => (
  <div className="text-neutral-500 text-sm">No context data available</div>
)

const WaterMonitoringConfidence: React.FC<{
  trend: RegionContextFactPack['water']['trend']
  state: RegionContextFactPack['water']['state']
}> = ({ trend, state }) => {
  const trendInsufficientPct = trend?.insufficientPct ?? 0
  const stateInsufficientPct =
    state?.bandDistribution?.['INSUFFICIENT'] !== undefined && state?.unitCount
      ? (state.bandDistribution['INSUFFICIENT'] / state.unitCount) * 100
      : 0

  const trendCount = trend?.unitCount ?? 0
  const stateCount = state?.unitCount ?? 0

  return (
    <div className="space-y-2">
      <h4 className="text-sm font-medium text-neutral-700">
        Water Monitoring Confidence
      </h4>
      <div className="grid grid-cols-2 gap-2 text-sm">
        <div className="bg-neutral-50 p-2 rounded">
          <div className="text-xs text-neutral-500">
            Trend: Monitoring Records
          </div>
          <div className="font-semibold">{trendCount}</div>
          <div className="text-xs text-neutral-400">
            Insufficient: {trendInsufficientPct.toFixed(1)}%
          </div>
        </div>
        <div className="bg-neutral-50 p-2 rounded">
          <div className="text-xs text-neutral-500">
            State: Monitoring Records
          </div>
          <div className="font-semibold">{stateCount}</div>
          <div className="text-xs text-neutral-400">
            Insufficient: {stateInsufficientPct.toFixed(1)}%
          </div>
        </div>
      </div>
    </div>
  )
}

const WaterDirectionSignal: React.FC<{
  trend: RegionContextFactPack['water']['trend']
}> = ({ trend }) => {
  if (!trend) return null

  return (
    <div className="space-y-2">
      <h4 className="text-sm font-medium text-neutral-700">
        Water Direction Signal
      </h4>
      <div className="grid grid-cols-3 gap-2 text-sm">
        <div className="bg-red-50 p-2 rounded border-l-4 border-red-500">
          <div className="text-xs text-neutral-500">Degrading</div>
          <div className="font-semibold">{trend.degradingPct.toFixed(1)}%</div>
        </div>
        <div className="bg-green-50 p-2 rounded border-l-4 border-green-500">
          <div className="text-xs text-neutral-500">Improving</div>
          <div className="font-semibold">{trend.improvingPct.toFixed(1)}%</div>
        </div>
        <div className="bg-neutral-50 p-2 rounded border-l-4 border-neutral-400">
          <div className="text-xs text-neutral-500">Indeterminate</div>
          <div className="font-semibold">
            {trend.indeterminatePct.toFixed(1)}%
          </div>
        </div>
      </div>
    </div>
  )
}

const WaterConditionSignal: React.FC<{
  state: RegionContextFactPack['water']['state']
}> = ({ state }) => {
  if (!state?.bandDistribution) return null

  const bands = ['A', 'B', 'C', 'D', 'E']
  const bandLabels: Record<string, string> = {
    A: 'Excellent',
    B: 'Good',
    C: 'Fair',
    D: 'Poor',
    E: 'Very Poor',
  }
  const bandColors: Record<string, string> = {
    A: 'bg-green-600',
    B: 'bg-green-500',
    C: 'bg-yellow-500',
    D: 'bg-orange-500',
    E: 'bg-red-600',
  }

  const total = state.unitCount || 1
  const dominant = bands.reduce(
    (max, band) =>
      (state.bandDistribution[band] || 0) > (state.bandDistribution[max] || 0)
        ? band
        : max,
    'A'
  )

  return (
    <div className="space-y-2">
      <h4 className="text-sm font-medium text-neutral-700">
        Water Condition Signal
      </h4>
      <div className="text-sm">
        <span className="font-medium">Dominant: </span>
        <span
          className={`px-2 py-0.5 rounded text-white ${bandColors[dominant]}`}
        >
          {dominant} - {bandLabels[dominant]}
        </span>
      </div>
      <div className="flex gap-1 h-8">
        {bands.map(band => {
          const count = state.bandDistribution[band] || 0
          const pct = (count / total) * 100
          return (
            <div
              key={band}
              className={`${bandColors[band]} flex-1 rounded-sm relative group`}
              style={{ minWidth: '20px' }}
              title={`Band ${band}: ${count} sites (${pct.toFixed(1)}%)`}
            />
          )
        })}
      </div>
      <div className="flex justify-between text-xs text-neutral-500">
        <span>A (Best)</span>
        <span>E (Worst)</span>
      </div>
    </div>
  )
}

const EnergySystemContext: React.FC<{
  energy: RegionContextFactPack['energy']
}> = ({ energy }) => {
  if (!energy) return null

  return (
    <div className="space-y-2">
      <h4 className="text-sm font-medium text-neutral-700">
        Energy System Context
      </h4>
      <div className="grid grid-cols-2 gap-2 text-sm">
        <div className="bg-green-50 p-2 rounded">
          <div className="text-xs text-neutral-500">Latest Renewable</div>
          <div className="font-semibold text-green-700">
            {energy.latestRenewablePct.toFixed(1)}%
          </div>
          <div className="text-xs text-neutral-400">({energy.latestYear})</div>
        </div>
        <div className="bg-neutral-50 p-2 rounded">
          <div className="text-xs text-neutral-500">5-Year Change</div>
          <div
            className={`font-semibold ${
              energy.renewable5YrDeltaPct >= 0
                ? 'text-green-600'
                : 'text-red-600'
            }`}
          >
            {energy.renewable5YrDeltaPct >= 0 ? '+' : ''}
            {energy.renewable5YrDeltaPct.toFixed(1)}%
          </div>
        </div>
        <div className="bg-gray-50 p-2 rounded col-span-2">
          <div className="text-xs text-neutral-500">Fossil Share (Latest)</div>
          <div className="font-semibold text-gray-700">
            {energy.fossilLatestPct.toFixed(1)}%
          </div>
        </div>
      </div>
    </div>
  )
}

const ContextSnapshot: React.FC<{
  data: RegionContextFactPack
  onExpand: () => void
}> = ({ data, onExpand }) => {
  const trend = data.water?.trend
  const state = data.water?.state
  const energy = data.energy

  const trendInsufficientPct = trend?.insufficientPct ?? 0
  const stateInsufficientPct =
    state?.bandDistribution?.['INSUFFICIENT'] !== undefined && state?.unitCount
      ? (state.bandDistribution['INSUFFICIENT'] / state.unitCount) * 100
      : 0
  const trendCount = trend?.unitCount ?? 0
  const stateCount = state?.unitCount ?? 0

  const bands = ['A', 'B', 'C', 'D', 'E']
  const dominantBand = bands.reduce(
    (max, band) =>
      (state?.bandDistribution?.[band] || 0) >
      (state?.bandDistribution?.[max] || 0)
        ? band
        : max,
    'A'
  )

  return (
    <div className="bg-neutral-50 rounded-lg p-3 mb-4">
      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
        <div>
          <span className="text-neutral-500">Trend: </span>
          <span className="font-medium">{trendCount} Monitoring Records</span>
          <span className="text-neutral-400"> • </span>
          <span className="text-neutral-500">
            {trendInsufficientPct.toFixed(1)}% insufficient
          </span>
        </div>
        <div>
          <span className="text-neutral-500">State: </span>
          <span className="font-medium">{stateCount} Monitoring Records</span>
          <span className="text-neutral-400"> • </span>
          <span className="text-neutral-500">
            {stateInsufficientPct.toFixed(1)}% insufficient
          </span>
        </div>
        <div>
          <span className="text-neutral-500">Direction: </span>
          <span className="font-medium">
            {trend?.degradingPct?.toFixed(1)}% degrading •{' '}
            {trend?.improvingPct?.toFixed(1)}% improving •{' '}
            {trend?.indeterminatePct?.toFixed(1)}% indeterminate
          </span>
        </div>
        <div>
          <span className="text-neutral-500">Condition: </span>
          <span className="font-medium">Dominant {dominantBand}</span>
        </div>
        <div>
          <span className="text-neutral-500">Energy: </span>
          <span className="font-medium">
            {energy?.latestRenewablePct?.toFixed(1)}% renewable
          </span>
          <span className="text-neutral-400"> • </span>
          <span
            className={
              energy?.renewable5YrDeltaPct && energy.renewable5YrDeltaPct >= 0
                ? 'text-green-600'
                : 'text-red-600'
            }
          >
            {energy?.renewable5YrDeltaPct && energy.renewable5YrDeltaPct >= 0
              ? '+'
              : ''}
            {energy?.renewable5YrDeltaPct?.toFixed(1)}% (5y)
          </span>
        </div>
      </div>
      <button
        onClick={onExpand}
        className="text-sm text-primary-600 hover:text-primary-800 mt-2 font-medium"
      >
        View Full Context →
      </button>
    </div>
  )
}

const RegionContextPanel: React.FC<RegionContextPanelProps> = ({
  region,
  indicator,
}) => {
  const [expanded, setExpanded] = useState(false)
  const { data, isLoading, error } = useRegionContext({
    region,
    indicator,
  })

  if (!region) {
    return null
  }

  if (isLoading) {
    return (
      <div className="mb-4">
        <LoadingSkeleton />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mb-4">
        <ErrorDisplay error={error as Error} />
      </div>
    )
  }

  if (!data) {
    return (
      <div className="mb-4">
        <EmptyDisplay />
      </div>
    )
  }

  if (!expanded) {
    return <ContextSnapshot data={data} onExpand={() => setExpanded(true)} />
  }

  return (
    <Card className="mb-6 border-l-4 border-l-primary-500">
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <h3 className="text-base font-semibold text-neutral-800">
          Regional Environmental Context
        </h3>
        <button
          onClick={() => setExpanded(false)}
          className="text-sm text-primary-600 hover:text-primary-800 font-medium"
        >
          ← Collapse
        </button>
      </CardHeader>
      <CardContent className="space-y-6">
        <WaterMonitoringConfidence
          trend={data.water?.trend}
          state={data.water?.state}
        />
        <hr className="border-neutral-200" />
        <WaterDirectionSignal trend={data.water?.trend} />
        <hr className="border-neutral-200" />
        <WaterConditionSignal state={data.water?.state} />
        <hr className="border-neutral-200" />
        <EnergySystemContext energy={data.energy} />
        <hr className="border-neutral-200" />
        <p className="text-xs text-neutral-500 italic">{DISCLAIMER_TEXT}</p>
      </CardContent>
    </Card>
  )
}

export default RegionContextPanel
