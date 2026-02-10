interface Logger {
  debug(message: string, ...args: unknown[]): void
  info(message: string, ...args: unknown[]): void
  warn(message: string, ...args: unknown[]): void
  error(message: string, ...args: unknown[]): void
}

class ConsoleLogger implements Logger {
  private isDev: boolean

  constructor() {
    this.isDev = import.meta.env.MODE === 'development'
  }

  debug(message: string, ...args: unknown[]): void {
    if (this.isDev) {
      console.debug(`[DEBUG] ${message}`, ...args)
    }
  }

  info(message: string, ...args: unknown[]): void {
    if (this.isDev) {
      console.info(`[INFO] ${message}`, ...args)
    }
  }

  warn(message: string, ...args: unknown[]): void {
    if (this.isDev) {
      console.warn(`[WARN] ${message}`, ...args)
    } else {
      console.warn(`[WARN] ${message}`)
    }
  }

  error(message: string, ...args: unknown[]): void {
    if (this.isDev) {
      console.error(`[ERROR] ${message}`, ...args)
    } else {
      console.error(`[ERROR] ${message}`)
    }
  }
}

export const logger: Logger = new ConsoleLogger()
export type { Logger }
