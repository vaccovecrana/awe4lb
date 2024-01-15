import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, apiV1ConfigListGet, apiV1ConfigSelectGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"
import { uiConfigEditFmt } from "@a4ui/util"

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

  private setActive(configId: string) {
    const {dispatch: d} = this.props.s
    const {configs} = this.state
    lockUi(true, d)
      .then(() => apiV1ConfigSelectGet(configId))
      .then((cfgState) => {
        if (cfgState.active) {
          configs.find(cfg => cfg.id === cfgState.active.id).active = true
        }
        if (cfgState.inactive) {
          configs.find(cfg => cfg.id === cfgState.inactive.id).active = false
        }
        this.setState({configs})
      })
      .then(() => lockUi(false, d))
  }

  public renderConfigRow(cfg: A4Config) {
    return (
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
          <div class="row justify-center align-center">
            <div class="col auto">
              <a href={cfg.active ? "/" : uiConfigEditFmt(cfg.id)}>
                <i class={cfg.active ? "icon-magnifier" : "icon-pencil"} />
              </a>
            </div>
            <div class="col auto">
              <button class="btn primary small" onClick={() => this.setActive(cfg.active ? "" : cfg.id)}>
                <b>
                  <i class={cfg.active ? "icon-control-pause" : "icon-control-end"}></i>
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
