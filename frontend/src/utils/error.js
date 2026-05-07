export function extractError(e) {
  if (!e) return '不明なエラーが発生しました'

  const data = e.response?.data
  const status = e.response?.status

  if (data !== undefined && data !== null) {
    if (typeof data === 'string' && data.trim()) return `[${status}] ${data}`
    if (typeof data === 'object') {
      if (data.message) return `[${status}] ${data.message}`
      if (data.error) return `[${status}] ${data.error}`
      return `[${status}] ${JSON.stringify(data)}`
    }
  }

  if (status) return `HTTPエラー ${status}: ${e.message}`
  return e.message || '不明なエラーが発生しました'
}
