// Frontend utilities for debugging and diagnostics
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export function generateRequestId(): string {
  return Math.random().toString(36).substring(2, 10)
}

export function classifyError(error: Error | Response): {
  type: string
  message: string
} {
  if (error instanceof TypeError && error.message.includes('fetch')) {
    return {
      type: 'NETWORK',
      message: 'Network error - unable to reach server',
    }
  }
  if (error instanceof TypeError && error.message.includes('CORS')) {
    return { type: 'CORS', message: 'CORS error - check server configuration' }
  }
  if ('status' in error) {
    const response = error as Response
    return {
      type: 'HTTP',
      message: `HTTP error ${response.status} ${response.statusText}`,
    }
  }
  return { type: 'UNKNOWN', message: error.message }
}

export async function getErrorResponseSnippet(
  response: Response
): Promise<string> {
  try {
    const text = await response.text()
    return text.length > 200 ? text.substring(0, 200) + '...' : text
  } catch {
    return '[Unable to read response body]'
  }
}
