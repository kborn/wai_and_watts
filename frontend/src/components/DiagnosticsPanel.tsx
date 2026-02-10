import { useState } from 'react'
import {
  getDiagnostics,
  clearDiagnostics,
  type ApiDiagnostics,
} from '../utils/diagnostics'

interface DiagnosticsPanelProps {
  isOpen: boolean
  onClose: () => void
}

export function DiagnosticsPanel({ isOpen, onClose }: DiagnosticsPanelProps) {
  const [diagnostics] = useState<ApiDiagnostics[]>(getDiagnostics())

  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed bottom-4 right-4 w-96 max-h-96 bg-gray-900 text-white p-4 rounded-lg shadow-lg overflow-hidden z-50">
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-sm font-mono font-bold">API Diagnostics (DEV)</h3>
        <div className="flex gap-2">
          <button
            onClick={clearDiagnostics}
            className="text-xs bg-gray-700 hover:bg-gray-600 px-2 py-1 rounded"
          >
            Clear
          </button>
          <button
            onClick={onClose}
            className="text-xs bg-gray-700 hover:bg-gray-600 px-2 py-1 rounded"
          >
            ×
          </button>
        </div>
      </div>

      <div className="overflow-y-auto max-h-80">
        {diagnostics.length === 0 ? (
          <p className="text-xs text-gray-400">No API calls recorded</p>
        ) : (
          <div className="space-y-2">
            {diagnostics.map((diag, index) => (
              <div
                key={index}
                className="bg-gray-800 p-2 rounded text-xs font-mono"
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="text-green-400">
                    {diag.requestId || 'unknown'}
                  </span>
                  <span
                    className={
                      diag.status
                        ? diag.status >= 400
                          ? 'text-red-400'
                          : 'text-green-400'
                        : 'text-red-400'
                    }
                  >
                    {diag.status || 'FAIL'}
                  </span>
                </div>

                <div className="text-gray-300 mb-1">
                  {diag.method || 'GET'} {diag.endpoint || 'unknown'}
                </div>

                {diag.errorType && (
                  <div className="text-red-400 mb-1">
                    {diag.errorType}: {diag.errorMessage}
                  </div>
                )}

                {diag.duration && (
                  <div className="text-gray-400">
                    {diag.duration}ms
                    {diag.timestamp &&
                      ` • ${new Date(diag.timestamp).toLocaleTimeString()}`}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

// Hook for toggling diagnostics panel
export function useDiagnostics() {
  const [isOpen, setIsOpen] = useState(false)

  const togglePanel = () => setIsOpen(!isOpen)

  return {
    isOpen,
    togglePanel,
    DiagnosticsPanel: () => (
      <DiagnosticsPanel isOpen={isOpen} onClose={() => setIsOpen(false)} />
    ),
  }
}
