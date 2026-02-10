interface LogInstructionsProps {
  isOpen: boolean
  onClose: () => void
}

export function LogInstructions({ isOpen, onClose }: LogInstructionsProps) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-2xl max-h-screen overflow-y-auto">
        <h2 className="text-xl font-bold mb-4">Where to Find Frontend Logs</h2>

        <div className="space-y-4 text-sm">
          <div className="bg-gray-50 p-4 rounded">
            <h3 className="font-semibold mb-2">Chrome/Edge:</h3>
            <ol className="list-decimal list-inside space-y-1 text-xs">
              <li>
                Press <kbd className="bg-gray-200 px-2 py-1 rounded">F12</kbd>{' '}
                or right-click → "Inspect"
              </li>
              <li>Click on "Console" tab</li>
              <li>Look for logs prefixed with [INFO], [ERROR], [WARN]</li>
              <li>
                API requests show as:{' '}
                <code className="bg-gray-200 px-1">
                  API Request/SUCCESS/Error: [requestId]
                </code>
              </li>
            </ol>
          </div>

          <div className="bg-gray-50 p-4 rounded">
            <h3 className="font-semibold mb-2">Firefox:</h3>
            <ol className="list-decimal list-inside space-y-1 text-xs">
              <li>
                Press <kbd className="bg-gray-200 px-2 py-1 rounded">F12</kbd>{' '}
                or right-click → "Inspect Element"
              </li>
              <li>Click on "Console" tab</li>
              <li>Look for logs prefixed with [INFO], [ERROR], [WARN]</li>
            </ol>
          </div>

          <div className="bg-blue-50 p-4 rounded">
            <h3 className="font-semibold mb-2">What to Look For:</h3>
            <ul className="list-disc list-inside space-y-1 text-xs">
              <li>
                <strong>API Request:</strong> Shows the request ID and endpoint
              </li>
              <li>
                <strong>API Success:</strong> Shows successful responses
              </li>
              <li>
                <strong>API Error/Failure:</strong> Shows errors with
                classification:
              </li>
              <ul className="list-circle list-inside ml-4 space-y-1 text-xs">
                <li>
                  <code className="bg-gray-200 px-1">NETWORK</code>: Server
                  unreachable
                </li>
                <li>
                  <code className="bg-gray-200 px-1">CORS</code>: Cross-origin
                  request blocked
                </li>
                <li>
                  <code className="bg-gray-200 px-1">HTTP</code>: Server
                  returned error status
                </li>
              </ul>
            </ul>
          </div>

          <div className="bg-yellow-50 p-4 rounded">
            <h3 className="font-semibold mb-2">Current Issue:</h3>
            <p className="text-xs">
              You're seeing "Failed to fetch" with 403 OPTIONS responses. This
              is a CORS issue. The backend CORS configuration has been added -
              restart the backend and try again.
            </p>
          </div>
        </div>

        <button
          onClick={onClose}
          className="mt-6 w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700"
        >
          Got it
        </button>
      </div>
    </div>
  )
}
