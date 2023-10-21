import * as React from "preact/compat"
import { A4Backend, A4Disc, A4Match, State } from "@a4ui/rpc"
import { matchLabel } from "@a4ui/util"

interface A4McProps { match: A4Match }

const renderBkState = (bk: A4Backend) => {
  const clazz = bk.state === State.Up ? "pill pill-green" : "pill pill-red"
  return <span class={clazz}>{bk.state}</span>
}

const renderDiscover = (discover: A4Disc) => {
  if (discover.http) {
    return (
      <div>
        <i class="icon-link mr4"></i>
        <a href={discover.http.endpoint}>{discover.http.endpoint}</a>
        <span class="pill pill-pale ml4">{discover.http.format}</span>
      </div>
    )
  } else if (discover.exec) {
    return (
      <div>
        <i class="icon-screen-desktop mr4"></i>
        <code>
          {discover.exec.command} {discover.exec.args.join(" ")}
        </code>
        <span class="pill pill-pale ml4">{discover.exec.format}</span>
      </div>
    )
  }
  return <div />
}

const renderIntervalTimeout = (intervalMs: number, timeoutMs: number) => {
  return (
    <div class="mt4">
      <i class="icon-loop mr4"></i><code>{intervalMs}ms</code>&nbsp;
      <i class="icon-clock mr4"></i><code>{timeoutMs}ms</code>
    </div>
  )
}

const A4MatchCard = (props: A4McProps) => (
  <div class="mt8">
    {(props.match.and || props.match.or) ? (
      <div class="txc match-cond">
        <code>{matchLabel(props.match)}</code>
      </div>
    ) : []}
    {(props.match.discover || props.match.healthCheck) ? (
      <div class="card minimal p8 mt8 txSmall">
        {props.match.discover ? [
          <div class="card-title-3">Discovery</div>,
          renderIntervalTimeout(props.match.discover.intervalMs, props.match.discover.timeoutMs),
          renderDiscover(props.match.discover)
        ] : []}
        {props.match.healthCheck ? [
          <hr />,
          <div class="card-title-3 mt8">Health check</div>,
          renderIntervalTimeout(props.match.healthCheck.intervalMs, props.match.healthCheck.timeoutMs)
        ] : []}
      </div>
    ) : []}
    {props.match.pool.hosts.length > 0 ? (
      <div class="card minimal mt8 txSmall">
        <div class="pltr8">
          <div class="row">
            <div class="col auto">
              <div class="card-title-3">Pool</div>
            </div>
            <div class="col auto">
              <div class="txr ph8">
                <i class="icon-directions mr4"></i>
                <code>{props.match.pool.type ? props.match.pool.type : "random"}</code>
              </div>
            </div>
          </div>
        </div>
        <table class="table">
          <thead>
            <th>host/port</th>
            <th>weight/priority</th>
            <th>state</th>
          </thead>
          <tbody>
            {props.match.pool.hosts.map(bk => (
              <tr>
                <td>{bk.addr.host}:{bk.addr.port}</td>
                <td>{bk.weight}/{bk.priority}</td>
                <td>
                  {renderBkState(bk)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ) : []}
  </div>
)

export default A4MatchCard
