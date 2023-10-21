import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Backend, A4Config, A4Server, State, apiV1ConfigList } from "@a4ui/rpc"
import { RenderableProps } from "preact"
import { matchLabel } from "@a4ui/util"

type A4DProps = RenderableProps<{ s?: A4Store }>
interface A4DState { configs?: A4Config[] }

class A4Dashboard extends React.Component<A4DProps, A4DState> {

  constructor() {
    super()
  }

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigList())
      .then(configs => this.setState({configs}))
      .then(() => lockUi(false, d))
  }

  public sortServers(servers: A4Server[]) {
    return servers.sort((a, b) => a.id.localeCompare(b.id))
  }

  private renderBkState(bk: A4Backend) {
    const clazz = bk.state === State.Up ? "pill pill-green" : "pill pill-red"
    return <span class={clazz}>{bk.state}</span>
  }

  public render() {
    return this.state.configs ? (
      <div class="p8">
        <h2>Configurations</h2>
        {this.state.configs.map((cfg) => (
          <div class="row">
            <div class="col auto">
              <div class="p8 card minimal">
                <div class="card-title-1 pv8">
                  {cfg.id}&nbsp;{cfg.active ? <small class="pill pill-green">active</small> : []}
                </div>
                <small>{cfg.description}</small>
                {cfg.servers.length > 0 ? ([
                  <div class="row">
                    {this.sortServers(cfg.servers).map(srv => (
                      <div class="col xs-12 sm-12 md-6">
                        <div class="card minimal p8 m2 mt8">
                          <div class="card-title-2">
                          <i class="icono-rss" /> {srv.id}
                          </div>
                          {srv.match.map((match, k) => (
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
                                            {this.renderBkState(bk)}
                                          </td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                              ) : []}
                              {srv.match.length > 1 && (k < srv.match.length - 1) ? <hr class="mt8" /> : []}
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ]) : []}
              </div>
            </div>
          </div>
        ))}
      </div>
    ) : (<div />)
  }

}

export default (props: A4DProps) => <A4Dashboard s={useContext(A4Context)} />
