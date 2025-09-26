import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Backend, A4BackendState, A4Config, A4Disc, A4HealthCheck, A4Match, A4Server, A4ServerTls, A4Tls, A4Udp, apiV1ConfigGet } from "@a4ui/rpc"
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

  public tr(th: string, td: string) {
    return (
      <tr>
        <th>{th}</th>
        <td>{td}</td>
      </tr>
    )
  }

  public cardCol(element: any) {
    return (
      <div class="col auto">
        <div class="card minimal mv8 mh4">
          {element}
        </div>
      </div>
    )
  }

  public renderDiscover(disc: A4Disc) {
    if (disc.exec) {
      return this.cardCol(
        <table class="table small">
          <tbody>
            {this.tr("Discover Interval", `${disc.intervalMs}ms`)}
            {this.tr("Discover Timeout", `${disc.timeoutMs}ms`)}
            {this.tr("Command", disc.exec.command)}
            {this.tr("Args", disc.exec.args.join(" "))}
            {this.tr("Format", disc.exec.format)}
          </tbody>
        </table>
      )
    }
    if (disc.http) {
      return this.cardCol(
        <table class="table small">
          <tbody>
            {this.tr("Discover Interval", `${disc.intervalMs}ms`)}
            {this.tr("Timeout", `${disc.timeoutMs}ms`)}
            {this.tr("Endpoint", disc.http.endpoint)}
            {this.tr("Format", disc.http.format)}
          </tbody>
        </table>
      )
    }
    return this.cardCol(
      <table class="table small">
        <tbody>
          {this.tr("Discover Interval", `${disc.intervalMs}ms`)}
          {this.tr("Timeout", `${disc.timeoutMs}ms`)}
          {this.tr("API server", disc.k8s.apiUri)}
          {this.tr("Namespace", disc.k8s.namespace)}
          {this.tr("Service/Port", `${disc.k8s.service}:${disc.k8s.port}`)}
          {this.tr("Token", disc.k8s.tokenPath)}
        </tbody>
      </table>
    )
  }

  public renderUdpTable(udp: A4Udp) {
    return this.cardCol(
      <table class="table small">
        <tbody>
          {this.tr("UDP timeout", `${udp.idleTimeoutMs}ms`)}
          {this.tr("Buffer Size", `${udp.bufferSize} bytes`)}
          {this.tr("Max Sessions", udp.maxSessions.toString())}
        </tbody>
      </table>
    )
  }

  public renderTlsTable(tls: A4ServerTls) {
    return this.cardCol(
      <table class="table small">
        <tbody>
          {tls.protocols && this.tr("Protocols", `${tls.protocols}`)}
          {tls.ciphers && this.tr("Ciphers", `${tls.ciphers}`)}
          {tls.base?.certPath && this.tr("Default certificate", tls.base.certPath)}
          {tls.base?.keyPath && this.tr("Default key", tls.base.keyPath)}
        </tbody>
      </table>
    )
  }

  public renderHealthCheckTable(health: A4HealthCheck) {
    if (health.exec) {
      return this.cardCol(
        <table class="table small">
          <tbody>
            {this.tr("Heatlh Interval", `${health.intervalMs}ms`)}
            {this.tr("Timeout", `${health.timeoutMs}ms`)}
            {this.tr("Command", health.exec.command)}
            {this.tr("Args", health.exec.args.join(" "))}
          </tbody>
        </table>
      )
    }
    return this.cardCol(
      <table class="table small">
        <tbody>
          {this.tr("Heatlh Interval", `${health.intervalMs}ms`)}
          {this.tr("Timeout", `${health.timeoutMs}ms`)}
        </tbody>
      </table>
    )
  }

  public renderMatchTlsTable(tls: A4Tls) {
    return this.cardCol(
      <table class="table small">
        <tbody>
          {this.tr("Cert path", tls.certPath)}
          {this.tr("Key path", tls.keyPath)}
          {this.tr("Backend trust", (tls.open ? tls.open : false).toString())}
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
        <div class="row">
          {srv.tls ? this.renderTlsTable(srv.tls) : []}
          {srv.udp ? this.renderUdpTable(srv.udp) : []}
        </div>
        {srv.match.map(match => (
          <div class="mt8">
            <div class="matchLabel">
              Match: <code>{matchLabelOf(match)} :: {match.pool.type ? match.pool.type : "random"}</code>
            </div>
            <div class="row">
              {match.tls ? this.renderMatchTlsTable(match.tls) : []}
              {match.healthCheck ? this.renderHealthCheckTable(match.healthCheck) : []}
              {match.discover ? this.renderDiscover(match.discover) : []}
            </div>
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
