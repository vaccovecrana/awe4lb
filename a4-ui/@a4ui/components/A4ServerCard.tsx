import * as React from "preact/compat"
import { A4Backend, A4Server, State } from "@a4ui/rpc"
import { matchLabel } from "@a4ui/util"

interface A4ScProps { srv: A4Server } 

const renderBkState = (bk: A4Backend) => {
  const clazz = bk.state === State.Up ? "pill pill-green" : "pill pill-red"
  return <span class={clazz}>{bk.state}</span>
}

const A4ServerCard = (props: A4ScProps) => (
  <div class="col xs-12 sm-12 md-6">
    <div class="card minimal p8 m2 mt8">
      <div class="card-title-2">
      <i class="icono-rss icoMed" /> {props.srv.id}
      </div>
      {props.srv.tls ? (
        <div class="mt8 txSmall card minimal">
          <div>
            <i class="icono-document icoSmall" /> <span>{props.srv.tls.certPath}</span>
          </div>
          <div>
            <i class="icono-asterisk icoSmall" /> <span>{props.srv.tls.keyPath}</span>
          </div>
          {props.srv.tls.ciphers && props.srv.tls.ciphers.length > 0 ? (
            <div>
              <i class="icono-hamburger icoSmall" /> {props.srv.tls.ciphers.join(", ")}
            </div>
          ) : []}
        </div>
      ) : []}
      {props.srv.match.map((match, k) => (
        <div class="mt8">
          {(match.and || match.or) ? (
            <div class="txc match-cond">
              <code>{matchLabel(match)}</code>
            </div>
          ) : []}
          {match.pool.hosts.length > 0 ? (
            <table class="table txSmall">
                <thead>
                  <th>host/port</th>
                  <th>weight/priority</th>
                  <th>state</th>
                </thead>
                <tbody>
                  {match.pool.hosts.map(bk => (
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
          {props.srv.match.length > 1 && (k < props.srv.match.length - 1) ? <hr class="mt8" /> : []}
        </div>
      ))}
    </div>
  </div>
)

export default A4ServerCard
