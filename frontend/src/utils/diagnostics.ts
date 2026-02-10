export interface ApiDiagnostics {
  requestId?: string
  endpoint?: string
  method?: string
  status?: number
  errorType?: string
  errorMessage?: string
  duration?: number
  timestamp?: string
}

let diagnostics: ApiDiagnostics[] = []

export function addDiagnostic(diagnostic: ApiDiagnostics): void {
  diagnostic.timestamp = new Date().toISOString()
  diagnostics.push(diagnostic)

  // Keep only the last 20 diagnostics
  if (diagnostics.length > 20) {
    diagnostics = diagnostics.slice(-20)
  }
}

export function getDiagnostics(): ApiDiagnostics[] {
  return [...diagnostics]
}

export function clearDiagnostics(): void {
  diagnostics = []
}
