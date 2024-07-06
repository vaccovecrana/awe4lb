import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, apiV1ConfigGet, apiV1ConfigListGet, apiV1ConfigSelectGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"
import { uiConfigEditFmt } from "@a4ui/util"

type A4CProps = RenderableProps<{ s?: A4Store }>
interface A4CState {
  active?: A4Config
  configs?: A4Config[]
}

class A4Configs extends React.Component<A4CProps, A4CState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => Promise.all([apiV1ConfigGet(undefined), apiV1ConfigListGet()]))
      .then(([active, configs]) => this.setState({...this.state, active, configs}))
      .then(() => lockUi(false, d))
  }

  private setActive(configId: string) {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigSelectGet(configId))
      .then(({active}) => this.setState({...this.state, active}))
      .then(() => lockUi(false, d))
  }

  public renderConfigRow(cfg: A4Config) {
    const active = this.state.active?.id === cfg.id
    return (
      <tr>
        <td>{cfg.id}</td>
        <td>{cfg.description}</td>
        <td>
          <span class={active ? "pill pill-green" : "pill pill-pale"}>
            {active ? "active" : "stopped"}
          </span>
        </td>
        <td>{cfg.servers.length}</td>
        <td>
          <div class="row justify-center align-center">
            <div class="col auto">
              <a href={active ? "/" : uiConfigEditFmt(cfg.id)}>
                <i class={active ? "icon-magnifier" : "icon-pencil"} />
              </a>
            </div>
            <div class="col auto">
              <button class="btn primary small" onClick={() => this.setActive(active ? undefined : cfg.id)}>
                <b>
                  <i class={active ? "icon-control-pause" : "icon-control-end"}></i>
                </b>
              </button>
            </div>
          </div>
        </td>
      </tr>
    )
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
                    {configs.map(cfg => this.renderConfigRow(cfg))}
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
