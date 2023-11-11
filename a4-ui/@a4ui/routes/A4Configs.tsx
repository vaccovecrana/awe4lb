import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, apiV1ConfigListGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"

type A4CProps = RenderableProps<{ s?: A4Store }>
interface A4CState { configs?: A4Config[] }

class A4Configs extends React.Component<A4CProps, A4CState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigListGet())
      .then(configs => this.setState({configs}))
      .then(() => lockUi(false, d))
  }

  public render() {
    const {configs} = this.state
    return (
      <div class="p8">
        <h2>Configurations</h2>
        <div class="row">
          <div class="col auto">
            <div class="card minimal">
              {configs ? (
                <table class="table txSmall">
                  <thead>
                    <th>ID</th>
                    <th>Description</th>
                    <th>Status</th>
                    <th>Servers</th>
                    <th>Actions</th>
                  </thead>
                  <tbody>
                    {configs.map(cfg => (
                      <tr>
                        <td>{cfg.id}</td>
                        <td>{cfg.description}</td>
                        <td>
                          <span class={cfg.active ? "pill pill-green" : "pill pill-pale"}>
                            {cfg.active ? "active" : "stopped"}
                          </span>
                        </td>
                        <td>{cfg.servers.length}</td>
                        <td>
                          <button class="btn primary small">
                            <b>
                              <i class={cfg.active ? "icon-control-pause" : "icon-control-end"}></i>
                            </b>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : []}
            </div>
          </div>
        </div>
      </div>
    )
  }

}

export default (props: A4CProps) => <A4Configs s={useContext(A4Context)} />
