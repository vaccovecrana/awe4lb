import * as React from "preact/compat"
import { A4Backend, A4Match, State } from "@a4ui/rpc"
import { matchLabel } from "@a4ui/util"

interface A4McProps { match: A4Match }

const renderBkState = (bk: A4Backend) => {
  const clazz = bk.state === State.Up ? "pill pill-green" : "pill pill-red"
  return <span class={clazz}>{bk.state}</span>
}

const A4MatchCard = (props: A4McProps) => (
  <div class="mt8">
    {(props.match.and || props.match.or) ? (
      <div class="txc match-cond">
        <code>{matchLabel(props.match)}</code>
      </div>
    ) : []}
    {props.match.discover ? (
      <div>Discover LOL</div>
    ) : []}
    {props.match.pool.hosts.length > 0 ? (
      <table class="table txSmall">
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
    ) : []}
  </div>
)

export default A4MatchCard
