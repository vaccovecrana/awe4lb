import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, apiV1ConfigList } from "@a4ui/rpc"
import { RenderableProps } from "preact"

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

  public render() {
    return this.state.configs ? (
      <div class="p8">
        <h2>Configurations</h2>
        {this.state.configs.map((cfg) => (
          <div class="row">
            <div class="col auto">
              <div class="p8 card minimal">
                <div class="txBold pv8">
                  {cfg.id}&nbsp;{cfg.active ? <small class="pill pill-green">active</small> : []}
                </div>
                <small>{cfg.description}</small>
                {cfg.servers.length > 0 ? (
                  <table class="table interactive mt8">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Bind</th>
                        <th>Matchers</th>
                      </tr>
                    </thead>
                    <tbody>
                      {cfg.servers.sort((a, b) => a.id.localeCompare(b.id)).map(srv => (
                        <tr>
                          <td>{srv.id}</td>
                          <td>{srv.addr.host}:{srv.addr.port}</td>
                          <td>{srv.match.length}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : []}
              </div>
            </div>
          </div>
        ))}
      </div>
    ) : (<div />)
  }

}

export default (props: A4DProps) => <A4Dashboard s={useContext(A4Context)} />
