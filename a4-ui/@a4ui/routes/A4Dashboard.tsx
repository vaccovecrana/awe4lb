import {useContext, useEffect, useState} from "preact/hooks"
import {A4Context, lockUi, setActiveConfig} from "@a4ui/store"
import {A4Config, A4Match, A4Server, apiV1ConfigGet} from "@a4ui/rpc"
import {formatBytes, formatDuration, matchLabelOf, serverTypeOf, uiConfigList, uiTraffic} from "@a4ui/util"
import {A4Router} from "@a4ui/components/A4Icons"
import A4StatusPill from "@a4ui/components/A4StatusPill"

interface Param {
  label: string
  value: string
  wide?: boolean
  mono?: boolean
}

const addParam = (params: Param[], label: string, value: any, options?: { wide?: boolean, mono?: boolean }) => {
  if (value === undefined || value === null || value === "") {
    return
  }
  params.push({label, value: String(value), ...options})
}

const serverParamsOf = (srv: A4Server): Param[] => {
  const params: Param[] = []
  if (srv.tls) {
    addParam(params, "Protocols", (srv.tls.protocols || []).join(", "))
    addParam(params, "Ciphers", (srv.tls.ciphers || []).join(", "), {wide: true, mono: true})
    addParam(params, "Default certificate", srv.tls.base?.certPath, {wide: true, mono: true})
    addParam(params, "Default key", srv.tls.base?.keyPath, {wide: true, mono: true})
  }
  if (srv.udp) {
    addParam(params, "UDP idle timeout", `${srv.udp.idleTimeoutMs} ms`)
    addParam(params, "Buffer size", formatBytes(srv.udp.bufferSize))
    addParam(params, "Max sessions", srv.udp.maxSessions)
  }
  return params
}

const matchParamsOf = (match: A4Match): Param[] => {
  const params: Param[] = []
  if (match.tls) {
    addParam(params, "Backend certificate", match.tls.certPath, {wide: true, mono: true})
    addParam(params, "Backend key", match.tls.keyPath, {wide: true, mono: true})
    addParam(params, "Backend trust", String(match.tls.open || false))
  }
  if (match.healthCheck) {
    addParam(params, "Health",
      `${formatDuration(match.healthCheck.intervalMs)} / ${formatDuration(match.healthCheck.timeoutMs)}`)
    if (match.healthCheck.exec) {
      addParam(params, "Health command",
        `${match.healthCheck.exec.command} ${(match.healthCheck.exec.args || []).join(" ")}`.trim(),
        {wide: true, mono: true})
    }
  }
  if (match.discover) {
    const d = match.discover
    const source = d.http ? "HTTP" : d.exec ? "Command" : d.k8s ? "Kubernetes" : "Discovery"
    const timing = d.intervalMs != null || d.timeoutMs != null
      ? `${formatDuration(d.intervalMs)} / ${formatDuration(d.timeoutMs)}`
      : ""
    addParam(params, "Discover", `${source}${timing ? ` · ${timing}` : ""}`)
    if (d.http) {
      addParam(params, "Endpoint", d.http.endpoint, {wide: true, mono: true})
    }
    if (d.exec) {
      addParam(params, "Discover command",
        `${d.exec.command} ${(d.exec.args || []).join(" ")}`.trim(), {wide: true, mono: true})
    }
    if (d.k8s) {
      addParam(params, "Kubernetes service",
        `${d.k8s.namespace}/${d.k8s.service}:${d.k8s.port}`, {wide: true, mono: true})
    }
  }
  return params
}

const A4Params = (props: { params: Param[] }) => (
  <div class="a4-params">
    {props.params.map((p, idx) => (
      <div class={`a4-param${p.wide ? " a4-param--wide" : ""}`} key={`${p.label}-${idx}`} title={p.value}>
        <span class="a4-param-label">{p.label}</span>
        <span class={`a4-param-value${p.mono ? " a4-mono" : ""}`}>{p.value}</span>
      </div>
    ))}
  </div>
)

const A4Dashboard = (props: { default?: boolean, path?: string }) => {
  const {dispatch} = useContext(A4Context)
  const [config, setConfig] = useState<A4Config>()
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    lockUi(true, dispatch)
    apiV1ConfigGet("")
      .then(cfg => {
        setConfig(cfg)
        setActiveConfig(cfg?.id, dispatch)
      })
      .catch(() => setConfig(undefined))
      .finally(() => {
        setLoaded(true)
        lockUi(false, dispatch)
      })
  }, [])

  if (!loaded) {
    return (
      <div>
        <div class="a4-skeleton a4-skeleton-card"/>
        <div class="a4-skeleton a4-skeleton-card a4-mt16"/>
      </div>
    )
  }

  if (!config || !config.id) {
    return (
      <section>
        <h1>Status</h1>
        <div class="a4-empty a4-mt16">
          <p>No active configuration.</p>
          <p class="a4-mt8"><a class="a4-pill a4-pill--accent" href={uiConfigList}>Choose a configuration</a></p>
        </div>
      </section>
    )
  }

  const servers = [...(config.servers || [])].sort((a, b) => (a.id || "").localeCompare(b.id || ""))

  return (
    <section>
      <div class="a4-hero-row">
        <div>
          <h1>{config.id}</h1>
          <div class="a4-subtitle">{config.description || "Active configuration"}</div>
        </div>
        <div style="margin-left:auto">
          <a class="a4-pill" href={uiTraffic}>Live traffic →</a>
        </div>
      </div>

      <div class="a4-mt16">
        {servers.length === 0 && <div class="a4-empty">This configuration has no servers.</div>}
        {servers.map((srv, idx) => {
          const type = serverTypeOf(srv)
          const srvParams = serverParamsOf(srv)
          return (
            <div class="a4-panel a4-mb-4" key={srv.id || idx}>
              <div class="a4-server-head">
                <div class="a4-server-title">
                  <A4Router maxHeight={24}/>
                  <h3>{srv.id || "(unnamed server)"}</h3>
                  <span class="a4-status a4-status-active">{type}</span>
                </div>
                <span class="a4-server-addr">{srv.addr?.host}:{srv.addr?.port}</span>
              </div>

              {srvParams.length > 0 && (
                <div class="a4-mb-4">
                  <div class="a4-section-title">Parameters</div>
                  <A4Params params={srvParams}/>
                </div>
              )}

              {(srv.match || []).map((match, mIdx) => {
                const params = matchParamsOf(match)
                const hosts = match.pool?.hosts || []
                return (
                  <div class="a4-match" key={`${srv.id}-${mIdx}`}>
                    <div class="a4-match-head">
                      <span class="a4-match-label">
                        <span class="a4-muted">Match</span>
                        <code>{matchLabelOf(match)}</code>
                      </span>
                      <span class="a4-tag">{match.pool?.type || "random"}</span>
                    </div>

                    <div class={`a4-match-body${params.length === 0 ? " a4-match-body--single" : ""}`}>
                      {params.length > 0 && (
                        <div class="a4-match-params">
                          <div class="a4-section-title">Parameters</div>
                          <A4Params params={params}/>
                        </div>
                      )}
                      <div class="a4-match-backends">
                        <div class="a4-section-title">
                          Backends <span class="a4-muted">({hosts.length})</span>
                        </div>
                        {hosts.length === 0 ? (
                          <div class="a4-form-text">No backends assigned yet.</div>
                        ) : (
                          <>
                            <div class="a4-table-wrap">
                              <table class="a4-table a4-table--compact">
                                <thead>
                                <tr>
                                  <th>Host / Port</th>
                                  <th>Weight / Priority</th>
                                  <th>State</th>
                                </tr>
                                </thead>
                                <tbody>
                                {hosts.map((bk, bIdx) => (
                                  <tr key={`${bk.addr?.host}:${bk.addr?.port}-${bIdx}`}>
                                    <td class="a4-mono">{bk.addr?.host}:{bk.addr?.port}</td>
                                    <td>{bk.weight ?? "-"} / {bk.priority ?? "-"}</td>
                                    <td><A4StatusPill state={bk.state}/></td>
                                  </tr>
                                ))}
                                </tbody>
                              </table>
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )
        })}
      </div>
    </section>
  )
}

export default A4Dashboard
