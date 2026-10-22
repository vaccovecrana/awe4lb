/** Extracts a user-facing message from the fetch rejections produced by rpc.ts. */
export const messageOf = (e: any): string => {
  if (e == null) {
    return "Unknown error"
  }
  const response = e.response as Response | undefined
  if (response && response.status) {
    return `Request failed (${response.status} ${response.statusText || ""})`.trim()
  }
  const cause = e.cause
  if (cause && cause.message) {
    return String(cause.message)
  }
  if (e.message) {
    return String(e.message)
  }
  return String(e)
}
