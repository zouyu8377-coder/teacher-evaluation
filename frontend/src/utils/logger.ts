/**
 * 日志工具
 * 提供统一的日志记录功能，支持日志级别和格式化输出
 */

type LogLevel = 0 | 1 | 2 | 3
const LogLevel = {
  DEBUG: 0 as 0,
  INFO: 1 as 1,
  WARN: 2 as 2,
  ERROR: 3 as 3
}

const levelNames = ['DEBUG', 'INFO', 'WARN', 'ERROR']

/**
 * 获取当前时间戳
 */
function getTimestamp(): string {
  return new Date().toISOString()
}

/**
 * 格式化日志消息
 */
function formatMessage(level: LogLevel, message: string, ...args: any[]): string {
  const timestamp = getTimestamp()
  const levelName = levelNames[level]
  let formatted = `[${timestamp}] [${levelName}] ${message}`

  if (args.length > 0) {
    formatted += ' ' + args.map(arg => {
      if (arg instanceof Error) {
        return `${arg.message}\n${arg.stack}`
      }
      if (typeof arg === 'object') {
        try {
          return JSON.stringify(arg, null, 2)
        } catch {
          return String(arg)
        }
      }
      return String(arg)
    }).join(' ')
  }

  return formatted
}

/**
 * 根据环境判断是否输出日志
 */
function shouldLog(level: LogLevel): boolean {
  // 生产环境只输出 warn 和 error
  if (import.meta.env.PROD) {
    return level >= LogLevel.WARN
  }
  return true
}

export const logger = {
  debug(message: string, ...args: any[]) {
    if (shouldLog(LogLevel.DEBUG)) {
      console.debug(formatMessage(LogLevel.DEBUG, message, ...args))
    }
  },

  info(message: string, ...args: any[]) {
    if (shouldLog(LogLevel.INFO)) {
      console.info(formatMessage(LogLevel.INFO, message, ...args))
    }
  },

  warn(message: string, ...args: any[]) {
    if (shouldLog(LogLevel.WARN)) {
      console.warn(formatMessage(LogLevel.WARN, message, ...args))
    }
  },

  error(message: string, ...args: any[]) {
    if (shouldLog(LogLevel.ERROR)) {
      console.error(formatMessage(LogLevel.ERROR, message, ...args))
    }
  }
}

/**
 * 便捷方法：记录API请求
 */
export function logRequest(config: any) {
  logger.debug(`[REQUEST] ${config.method?.toUpperCase()} ${config.url}`, {
    params: config.params,
    data: config.data
  })
}

/**
 * 便捷方法：记录API响应
 */
export function logResponse(response: any) {
  logger.debug(`[RESPONSE] ${response.config?.url}`, {
    status: response.status,
    data: response.data
  })
}

/**
 * 便捷方法：记录API错误
 */
export function logError(error: any) {
  logger.error(`[ERROR] ${error.config?.url}`, {
    message: error.message,
    status: error.response?.status,
    data: error.response?.data
  })
}

export default logger