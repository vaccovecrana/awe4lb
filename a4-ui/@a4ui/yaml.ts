import {parseDocument, stringify} from "yaml"
import {A4Config, A4Disc, A4Format, A4HealthCheck, A4Match, A4PoolType, A4Server, A4Tls} from "@a4ui/rpc"
import {numOrUndefined, splitArgs, splitList} from "@a4ui/util"

export type PoolType = "" | "roundRobin" | "leastConn" | "ipHash" | "weight"
type OpKind = "any" | "host" | "sni"
type OpType = "equals" | "startsWith" | "endsWith"
type DiscoverKind = "http" | "exec" | "k8s"
export type ServerTransport = "tcp" | "tls" | "udp"

export interface HostEntry {
  key: string
  host: string
  port: string
  weight: string
  priority: string
}

interface OpEntry {
  kind: OpKind
  op: OpType
  value: string
}

export interface DiscoverEntry {
  enabled: boolean
  kind: DiscoverKind
  intervalMs: string
  timeoutMs: string
  endpoint: string
  format: A4Format
  command: string
  args: string
  apiUri: string
  tokenPath: string
  namespace: string
  service: string
  port: string
}

export interface HealthEntry {
  enabled: boolean
  intervalMs: string
  timeoutMs: string
  command: string
  args: string
}

export interface TlsEntry {
  enabled: boolean
  certPath: string
  keyPath: string
  open: boolean
}

export interface MatchEntry {
  key: string
  op: OpEntry
  poolType: PoolType
  hosts: HostEntry[]
  discover: DiscoverEntry
  health: HealthEntry
  tls: TlsEntry
}

export interface ServerEntry {
  key: string
  id: string
  host: string
  port: string
  transport: ServerTransport
  tlsProtocols: string
  tlsCiphers: string
  tlsBase: TlsEntry
  tcpMaxConnectionsPerIp: string
  tcpTlsHandshakeTimeoutMs: string
  tcpMaxTlsHandshakes: string
  udpBufferSize: string
  udpIdleTimeoutMs: string
  udpMaxSessions: string
  matches: MatchEntry[]
}

export interface ConfigForm {
  id: string
  description: string
  servers: ServerEntry[]
}

interface YamlParseResult {
  form?: ConfigForm
  error?: string
  warnings: string[]
}

let keySeq = 0
const nextKey = (prefix = "k") => `${prefix}-${++keySeq}`

export const emptyHost = (host = "", port = ""): HostEntry => ({
  key: nextKey("host"), host, port, weight: "", priority: ""
})

export const emptyDiscover = (): DiscoverEntry => ({
  enabled: false,
  kind: "http",
  intervalMs: "",
  timeoutMs: "",
  endpoint: "",
  format: A4Format.json,
  command: "",
  args: "",
  apiUri: "",
  tokenPath: "",
  namespace: "",
  service: "",
  port: ""
})

export const emptyHealth = (): HealthEntry => ({
  enabled: false, intervalMs: "", timeoutMs: "", command: "", args: ""
})

export const emptyTls = (): TlsEntry => ({
  enabled: false, certPath: "", keyPath: "", open: false
})

export const emptyMatch = (): MatchEntry => ({
  key: nextKey("match"),
  op: {kind: "any", op: "equals", value: ""},
  poolType: "roundRobin",
  hosts: [emptyHost()],
  discover: emptyDiscover(),
  health: emptyHealth(),
  tls: emptyTls()
})

export const emptyServer = (): ServerEntry => ({
  key: nextKey("server"),
  id: "",
  host: "0.0.0.0",
  port: "",
  transport: "tcp",
  tlsProtocols: "",
  tlsCiphers: "",
  tlsBase: emptyTls(),
  tcpMaxConnectionsPerIp: "512",
  tcpTlsHandshakeTimeoutMs: "5000",
  tcpMaxTlsHandshakes: "256",
  udpBufferSize: "16384",
  udpIdleTimeoutMs: "60000",
  udpMaxSessions: "4096",
  matches: [emptyMatch()]
})

export const newConfigForm = (): ConfigForm => {
  const server = emptyServer()
  server.id = "http"
  server.port = "8080"
  server.matches[0].hosts = [emptyHost("127.0.0.1", "3000")]
  return {id: "", description: "", servers: [server]}
}

const opKindOf = (m: A4Match): OpKind => m.op?.host ? "host" : m.op?.sni ? "sni" : "any"

const opTypeOf = (op: any): OpType => {
  if (!op) return "equals"
  if (op.startsWith !== undefined) return "startsWith"
  if (op.endsWith !== undefined) return "endsWith"
  return "equals"
}

const opValueOf = (op: any): string => {
  if (!op) return ""
  if (op.startsWith !== undefined) return String(op.startsWith)
  if (op.endsWith !== undefined) return String(op.endsWith)
  return op.equals !== undefined ? String(op.equals) : ""
}

const discoverKindOf = (d: A4Disc): DiscoverKind =>
  d.exec ? "exec" : d.k8s ? "k8s" : "http"

export const fromConfig = (cfg: A4Config | undefined, previous?: ConfigForm): ConfigForm => {
  const prevServers = previous?.servers || []
  const servers = (cfg?.servers || []).map((srv, sIdx) => {
    const prevServer = prevServers[sIdx]
    const prevMatches = prevServer?.matches || []
    return {
      key: prevServer?.key || nextKey("server"),
      id: srv.id || "",
      host: srv.addr?.host || "",
      port: srv.addr?.port != null ? String(srv.addr.port) : "",
      transport: (srv.tls ? "tls" : srv.udp ? "udp" : "tcp") as ServerTransport,
      tlsProtocols: (srv.tls?.protocols || []).join(", "),
      tlsCiphers: (srv.tls?.ciphers || []).join(", "),
      tlsBase: {
        enabled: !!srv.tls?.base,
        certPath: srv.tls?.base?.certPath || "",
        keyPath: srv.tls?.base?.keyPath || "",
        open: !!srv.tls?.base?.open
      },
      tcpMaxConnectionsPerIp: srv.tcp?.maxConnectionsPerIp != null
        ? String(srv.tcp.maxConnectionsPerIp) : "512",
      tcpTlsHandshakeTimeoutMs: srv.tcp?.tlsHandshakeTimeoutMs != null
        ? String(srv.tcp.tlsHandshakeTimeoutMs) : "5000",
      tcpMaxTlsHandshakes: srv.tcp?.maxTlsHandshakes != null
        ? String(srv.tcp.maxTlsHandshakes) : "256",
      udpBufferSize: srv.udp?.bufferSize != null ? String(srv.udp.bufferSize) : "16384",
      udpIdleTimeoutMs: srv.udp?.idleTimeoutMs != null ? String(srv.udp.idleTimeoutMs) : "60000",
      udpMaxSessions: srv.udp?.maxSessions != null ? String(srv.udp.maxSessions) : "4096",
      matches: (srv.match || []).map((m, mIdx) => {
        const prevMatch = prevMatches[mIdx]
        const prevHosts = prevMatch?.hosts || []
        const kind = discoverKindOf(m.discover || {})
        return {
          key: prevMatch?.key || nextKey("match"),
          op: {
            kind: opKindOf(m),
            op: opTypeOf(m.op?.host || m.op?.sni),
            value: opValueOf(m.op?.host || m.op?.sni)
          },
          poolType: (m.pool?.type || "") as PoolType,
          hosts: (m.pool?.hosts || []).map((bk, bIdx) => ({
            key: prevHosts[bIdx]?.key || nextKey("host"),
            host: bk.addr?.host || "",
            port: bk.addr?.port != null ? String(bk.addr.port) : "",
            weight: bk.weight != null ? String(bk.weight) : "",
            priority: bk.priority != null ? String(bk.priority) : ""
          })),
          discover: {
            enabled: !!m.discover,
            kind,
            intervalMs: m.discover?.intervalMs != null ? String(m.discover.intervalMs) : "",
            timeoutMs: m.discover?.timeoutMs != null ? String(m.discover.timeoutMs) : "",
            endpoint: m.discover?.http?.endpoint || "",
            format: (m.discover?.http?.format || m.discover?.exec?.format || "json") as A4Format,
            command: m.discover?.exec?.command || "",
            args: (m.discover?.exec?.args || []).join(" "),
            apiUri: m.discover?.k8s?.apiUri || "",
            tokenPath: m.discover?.k8s?.tokenPath || "",
            namespace: m.discover?.k8s?.namespace || "",
            service: m.discover?.k8s?.service || "",
            port: m.discover?.k8s?.port != null ? String(m.discover.k8s.port) : ""
          },
          health: {
            enabled: !!m.healthCheck,
            intervalMs: m.healthCheck?.intervalMs != null ? String(m.healthCheck.intervalMs) : "",
            timeoutMs: m.healthCheck?.timeoutMs != null ? String(m.healthCheck.timeoutMs) : "",
            command: m.healthCheck?.exec?.command || "",
            args: (m.healthCheck?.exec?.args || []).join(" ")
          },
          tls: {
            enabled: !!m.tls,
            certPath: m.tls?.certPath || "",
            keyPath: m.tls?.keyPath || "",
            open: !!m.tls?.open
          }
        }
      })
    }
  })
  return {
    id: cfg?.id || "",
    description: cfg?.description || "",
    servers
  }
}

const tlsOf = (t: TlsEntry): A4Tls => ({
  certPath: t.certPath || undefined,
  keyPath: t.keyPath || undefined,
  open: t.open
})

const hostOf = (h: HostEntry) => {
  const out: any = {addr: {host: h.host || undefined, port: numOrUndefined(h.port)}}
  const weight = numOrUndefined(h.weight)
  const priority = numOrUndefined(h.priority)
  if (weight !== undefined && priority !== undefined) {
    out.weight = weight
    out.priority = priority
  }
  return out
}

const matchOf = (m: MatchEntry, transport: ServerTransport): A4Match => {
  const out: A4Match = {}
  if (m.op.kind !== "any") {
    const op = {[m.op.op]: m.op.value || undefined}
    out.op = m.op.kind === "host" ? {host: op} : {sni: op}
  }
  const pool: any = {}
  if (m.poolType) {
    pool.type = m.poolType as A4PoolType
  }
  if (!m.discover.enabled) {
    pool.hosts = m.hosts
      .filter(h => h.host.trim() || h.port.trim())
      .map(hostOf)
  }
  out.pool = pool
  if (m.discover.enabled) {
    const d: A4Disc = {}
    const intervalMs = numOrUndefined(m.discover.intervalMs)
    const timeoutMs = numOrUndefined(m.discover.timeoutMs)
    if (intervalMs !== undefined) d.intervalMs = intervalMs
    if (timeoutMs !== undefined) d.timeoutMs = timeoutMs
    if (m.discover.kind === "http") {
      d.http = {endpoint: m.discover.endpoint || undefined, format: m.discover.format}
    } else if (m.discover.kind === "exec") {
      d.exec = {
        command: m.discover.command || undefined,
        args: splitArgs(m.discover.args),
        format: m.discover.format
      }
    } else {
      d.k8s = {
        apiUri: m.discover.apiUri || undefined,
        tokenPath: m.discover.tokenPath || undefined,
        namespace: m.discover.namespace || undefined,
        service: m.discover.service || undefined,
        port: numOrUndefined(m.discover.port)
      }
    }
    out.discover = d
  }
  if (m.health.enabled) {
    const h: A4HealthCheck = {}
    const intervalMs = numOrUndefined(m.health.intervalMs)
    const timeoutMs = numOrUndefined(m.health.timeoutMs)
    if (intervalMs !== undefined) h.intervalMs = intervalMs
    if (timeoutMs !== undefined) h.timeoutMs = timeoutMs
    if (m.health.command.trim()) {
      h.exec = {command: m.health.command, args: splitArgs(m.health.args)}
    }
    out.healthCheck = h
  }
  if (m.tls.enabled && transport !== "udp") {
    out.tls = tlsOf(m.tls)
  }
  return out
}

const serverOf = (s: ServerEntry): A4Server => {
  const out: A4Server = {
    id: s.id || undefined,
    addr: {host: s.host || undefined, port: numOrUndefined(s.port)},
    match: s.matches.map(m => matchOf(m, s.transport))
  }
  if (s.transport === "tls") {
    out.tls = {}
    const protocols = splitList(s.tlsProtocols)
    const ciphers = splitList(s.tlsCiphers)
    if (protocols.length > 0) out.tls.protocols = protocols
    if (ciphers.length > 0) out.tls.ciphers = ciphers
    if (s.tlsBase.enabled) out.tls.base = tlsOf(s.tlsBase)
  }
  if (s.transport !== "udp") {
    out.tcp = {
      maxConnectionsPerIp: numOrUndefined(s.tcpMaxConnectionsPerIp),
      tlsHandshakeTimeoutMs: numOrUndefined(s.tcpTlsHandshakeTimeoutMs),
      maxTlsHandshakes: numOrUndefined(s.tcpMaxTlsHandshakes)
    }
  }
  if (s.transport === "udp") {
    out.udp = {
      bufferSize: numOrUndefined(s.udpBufferSize),
      idleTimeoutMs: numOrUndefined(s.udpIdleTimeoutMs),
      maxSessions: numOrUndefined(s.udpMaxSessions)
    } as any
  }
  return out
}

export const toConfig = (form: ConfigForm): A4Config => ({
  id: form.id.trim() || undefined,
  description: form.description || undefined,
  servers: form.servers.map(serverOf)
})

export const yamlOf = (form: ConfigForm): string =>
  stringify(toConfig(form), {lineWidth: 120})

export const parseConfigYaml = (text: string, previous?: ConfigForm): YamlParseResult => {
  const doc = parseDocument(text, {prettyErrors: true})
  const warnings = doc.warnings.map(w => w.message)
  if (doc.errors.length > 0) {
    const e = doc.errors[0]
    const pos = e.linePos?.[0]
    return {
      error: pos ? `${e.message} (line ${pos.line}, column ${pos.col})` : e.message,
      warnings
    }
  }
  let obj: any
  try {
    obj = doc.toJS()
  } catch (e: any) {
    return {error: e?.message || String(e), warnings}
  }
  if (obj == null || typeof obj !== "object" || Array.isArray(obj)) {
    return {error: "Configuration must be a YAML mapping", warnings}
  }
  return {form: fromConfig(obj as A4Config, previous), warnings}
}
