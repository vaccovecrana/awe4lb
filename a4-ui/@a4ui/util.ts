import {A4BackendState, A4Match, A4Server} from "@a4ui/rpc"

export const classNames = (...names: (string | false | null | undefined)[]): string =>
  names.filter(Boolean).join(" ")

export const uiRoot = "/"
export const uiConfigList = "/config/list"
export const uiConfigEdit = "/config/:configId/edit"
export const uiTraffic = "/traffic"

export const uiConfigEditFmt = (configId: string) => `/config/${configId}/edit`

const stringOpLabelOf = (op: any): string => {
  if (!op) {
    return "?"
  }
  const opId =
    op.endsWith ? "endsWith" :
    op.equals ? "equals" :
    op.startsWith ? "startsWith" :
    "?"
  const opVal =
    op.endsWith ? op.endsWith :
    op.equals ? op.equals :
    op.startsWith ? op.startsWith :
    "?"
  return `${opId}(${opVal})`
}

const matchOpLabelOf = (op: any): string => {
  const varId =
    op.host ? "host" :
    op.sni ? "sni" : "?"
  return `${varId} ${stringOpLabelOf(op.host || op.sni)}`
}

export const matchLabelOf = (match: A4Match): string => {
  if (!match || match.op === undefined) {
    return "any"
  }
  return matchOpLabelOf(match.op)
}

export const serverTypeOf = (srv: A4Server): string => {
  if (srv.udp) {
    return "UDP"
  }
  if (srv.tls) {
    return "TLS"
  }
  return "TCP"
}

export const statusClass = (state?: string | A4BackendState): string => {
  const s = String(state || "unknown").toLowerCase()
  if (s === "up" || s === "running" || s === "active") return "a4-status a4-status-up"
  if (s === "down" || s === "failed") return "a4-status a4-status-down"
  return "a4-status a4-status-unknown"
}

export const formatNumber = (n: number | undefined): string =>
  n == null || !Number.isFinite(n) ? "0" : Math.round(n).toLocaleString()

export const formatBytes = (n: number | undefined): string => {
  if (n == null || !Number.isFinite(n) || n <= 0) {
    return "0 B"
  }
  const units = ["B", "KiB", "MiB", "GiB", "TiB"]
  let value = n
  let idx = 0
  while (value >= 1024 && idx < units.length - 1) {
    value = value / 1024
    idx = idx + 1
  }
  return `${value >= 10 || idx === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[idx]}`
}

export const formatRate = (n: number | undefined): string =>
  n == null || !Number.isFinite(n) || n <= 0 ? "0 B/s" : `${formatBytes(n)}/s`

export const formatDuration = (ms: number | undefined): string => {
  if (ms == null || !Number.isFinite(ms)) {
    return "?"
  }
  if (ms < 1000) {
    return `${Math.round(ms)} ms`
  }
  const seconds = ms / 1000
  return `${Number.isInteger(seconds) ? seconds.toFixed(0) : seconds.toFixed(1)}s`
}

export const formatTime = (ms: number | undefined): string => {
  if (ms == null) {
    return "?"
  }
  return new Date(ms).toLocaleTimeString()
}

/** Shell-like split for command arguments, honouring single and double quotes. */
export const splitArgs = (text: string): string[] => {
  const out: string[] = []
  let current = ""
  let quote: string | undefined
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (quote) {
      if (ch === quote) {
        quote = undefined
      } else {
        current += ch
      }
    } else if (ch === "\"" || ch === "'") {
      quote = ch
    } else if (/\s/.test(ch)) {
      if (current) {
        out.push(current)
        current = ""
      }
    } else {
      current += ch
    }
  }
  if (current) {
    out.push(current)
  }
  return out
}

export const splitList = (text: string): string[] =>
  text.split(/[,\n]/).map(v => v.trim()).filter(Boolean)

export const numOrUndefined = (text: string): number | undefined => {
  const v = text.trim()
  if (!v) {
    return undefined
  }
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}
