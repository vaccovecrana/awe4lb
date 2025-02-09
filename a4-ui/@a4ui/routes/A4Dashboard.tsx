import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Backend, A4BackendState, A4Config, A4Disc, A4HealthCheck, A4Match, A4MatchTls, A4Server, A4ServerTls, A4Udp, apiV1ConfigGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"

import { A4Router } from "@a4ui/components/A4Icons"
import { matchLabelOf, serverTypeOf } from "@a4ui/util"

type A4DProps = RenderableProps<{ s?: A4Store }>
interface A4DState { config?: A4Config }

class A4Dashboard extends React.Component<A4DProps, A4DState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigGet(undefined))
      .then(config => this.setState({config}))
      .then(() => lockUi(false, d))
  }

  public renderBkState(bk: A4Backend) {
    const clazz = bk.state === A4BackendState.Up ? "pill pill-green" : "pill pill-red"
    return <span class={clazz}>{bk.state}</span>
  }  

  public renderPoolHostsTable(match: A4Match) {
    if (!match.pool?.hosts || match.pool.hosts.length === 0) {
      return <div />
    }
    return (
      <table class="table small">
        <thead>
          <th>Host/Port</th>
          <th>Weight/Priority</th>
          <th>State</th>
        </thead>
        <tbody>
          {match.pool.hosts.map(bk => (
            <tr>
              <td>{bk.addr.host}:{bk.addr.port}</td>
              <td>{bk.weight}/{bk.priority}</td>
              <td>{this.renderBkState(bk)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )
  }

  public renderDiscover(disc: A4Disc) {
    if (disc.exec) {
      return (
        <table class="table small">
          <thead>
            <th>Discover Interval/Timeout</th>
            <th>Command/Args</th>
            <th>Format</th>
          </thead>
          <tbody>
            <tr>
              <td>{disc.intervalMs}ms, {disc.timeoutMs}ms</td>
              <td>{disc.exec.command} {disc.exec.args.join(" ")}</td>
              <td>{disc.exec.format}</td>
            </tr>
          </tbody>
        </table>
      )
    }
    if (disc.http) {
      return (
        <table class="table small">
          <thead>
            <th>Discover Interval</th>
            <th>Timeout</th>
            <th>Endpoint</th>
            <th>Format</th>
          </thead>
          <tbody>
            <tr>
              <td>{disc.intervalMs}ms</td>
              <td>{disc.timeoutMs}ms</td>
              <td><code>{disc.http.endpoint}</code></td>
              <td><code>{disc.http.format}</code></td>
            </tr>
          </tbody>
        </table>
      )
    }
    return (
      <table class="table small">
        <thead>
          <th>Discover Interval</th>
          <th>Timeout</th>
          <th>API server</th>
          <th>Namespace</th>
          <th>Service/Port</th>
          <th>Token</th>
        </thead>
        <tbody>
          <tr>
            <td>{disc.intervalMs}ms</td>
            <td>{disc.timeoutMs}ms</td>
            <td><code>{disc.k8s.apiUri}</code></td>
            <td><code>{disc.k8s.namespace}</code></td>
            <td><code>{disc.k8s.service}:{disc.k8s.port}</code></td>
            <td><code>{disc.k8s.tokenPath}</code></td>
          </tr>
        </tbody>
      </table>
    )
  }

  public renderUdpTable(udp: A4Udp) {
    return (
      <table class="table small">
        <thead>
          <th>UDP timeout</th>
          <th>Buffer Size</th>
          <th>Max Sessions</th>
        </thead>
        <tbody>
          <tr>
            <td>{udp.idleTimeoutMs}ms</td>
            <td>{udp.bufferSize} bytes</td>
            <td>{udp.maxSessions}</td>
          </tr>
        </tbody>
      </table>
    )
  }

  public renderTlsTable(tls: A4ServerTls) {
    return (
      <table class="table small">
        <thead>
          <th>Protocols</th>
          <th>Ciphers</th>
        </thead>
        <tbody>
          <tr>
            <td>{tls.protocols}</td>
            <td>{tls.ciphers}</td>
          </tr>
        </tbody>
      </table>
    )
  }

  public renderHealthCheckTable(health: A4HealthCheck) {
    if (health.exec) {
      return (
        <table class="table small">
          <thead>
            <th>Heatlh Interval</th>
            <th>Timeout</th>
            <th>Command/Args</th>
          </thead>
          <tbody>
            <tr>
              <td>{health.intervalMs}ms</td>
              <td>{health.timeoutMs}ms</td>
              <td>
                <code>{health.exec.command} {health.exec.args.join(" ")}</code>
              </td>
            </tr>
          </tbody>
        </table>
      )
    }
    return (
      <table class="table small">
        <thead>
          <th>Health Interval</th>
          <th>Timeout</th>
        </thead>
        <tbody>
          <tr>
            <td>{health.intervalMs}ms</td>
            <td>{health.timeoutMs}ms</td>
          </tr>
        </tbody>
      </table>
    )
  }

  public renderMatchTlsTable(tls: A4MatchTls) {
    return (
      <table class="table small">
        <thead>
          <th>Cert path</th>
          <th>Key path</th>
          <th>Backend trust</th>
        </thead>
        <tbody>
          <tr>
            <td>{tls.certPath}</td>
            <td>{tls.keyPath}</td>
            <td>{tls.open ? tls.open : "false"}</td>
          </tr>
        </tbody>
      </table>
    )
  }

  public renderServerCard(srv: A4Server) {
    return (
      <div class="card minimal mt16 p8">
        <div class="hFlexBorder">
          <div class="row align-center">
            <div class="col auto">
              <h4 class="hFlex">
                <A4Router maxHeight={24} />
                <span class="pl8">{srv.id}</span>
              </h4>
            </div>
            <div class="col auto txr">
              <code class="mh4">
                {serverTypeOf(srv)}/{srv.addr.host}:{srv.addr.port}
              </code>
            </div>
          </div>
        </div>
        {srv.tls ? this.renderTlsTable(srv.tls) : []}
        {srv.udp ? this.renderUdpTable(srv.udp) : []}
        {srv.match.map(match => (
          <div class="mt8">
            <div class="matchLabel">
              Match: <code>{matchLabelOf(match)} :: {match.pool.type ? match.pool.type : "random"}</code>
            </div>
            {match.tls ? this.renderMatchTlsTable(match.tls) : []}
            {match.healthCheck ? this.renderHealthCheckTable(match.healthCheck) : []}
            {match.discover ? this.renderDiscover(match.discover) : []}
            {this.renderPoolHostsTable(match)}
          </div>
        ))}
      </div>
    )
  }

  public render() {
    var {config: cfg} = this.state
    var servers = cfg && cfg.id
      ? cfg.servers.sort((a, b) => a.id.localeCompare(b.id))
      : []
    return (
      <div>
        <h1>Active config</h1>
        <div class="row">
          <div class="col auto">
            {servers.length > 0 ? (
              <div class="mt16">
                <h3>{cfg.id}</h3>
                <div class="callout mt16">
                  <small>{cfg.description}</small>
                </div>
                <h3 class="mt16">Servers</h3>
                {servers.map(srv => this.renderServerCard(srv))}
              </div>
            ) : (
              <div class="frame txc p16">No active config</div>
            )}
          </div>
        </div>
      </div>
    )
  }

}

export default (props: A4DProps) => <A4Dashboard s={useContext(A4Context)} />
