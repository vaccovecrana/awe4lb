import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, apiV1ConfigGet, apiV1ConfigListGet, apiV1ConfigSelectGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"
import { uiConfigEditFmt } from "@a4ui/util"
import { A4IcnAdd, A4IcnPlay, A4IcnStop, A4IconEdit, A4Inspect } from "@a4ui/components/A4Icons"

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

  public renderControls(active: boolean, cfg: A4Config) {
    return (
      <div>
        <div class="row justify-center align-center">
          <div class="col auto txc">
            <a href={active ? "/" : uiConfigEditFmt(cfg.id)}>
              {active ? <A4Inspect maxHeight={28} /> : <A4IconEdit maxHeight={28} />}
            </a>
          </div>
          <div class="col auto txc">
            <a class="ptr" onClick={() => this.setActive(active ? undefined : cfg.id)}>
              {active ? <A4IcnStop maxHeight={28} /> : <A4IcnPlay maxHeight={28} />}
            </a>
          </div>
        </div>
      </div>
    )
  }

  public renderStatus(active: boolean) {
    return (
      <div class={active ? "pill pill-green" : "pill pill-pale"}>
        {active ? "active" : "stopped"}
      </div>
    )
  }

  public renderConfigRow(cfg: A4Config) {
    const active = this.state.active?.id === cfg.id
    return (
      <tr>
        <td>{cfg.id}</td>
        <td>{cfg.description}</td>
        <td>{this.renderStatus(active)}</td>
        <td>{cfg.servers.length}</td>
        <td>{this.renderControls(active, cfg)}</td>
      </tr>
    )
  }

  public renderTable(configs: A4Config[]) {
    return (
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
    )
  }

  public renderCard(cfg: A4Config) {
    const active = this.state.active?.id === cfg.id
    return (
      <div class="card minimal mt16">
        <div class="card-body">
          <h4>
            <div class="row align-center">
              <div class="col xs-9 sm-9">
                {cfg.id}
              </div>
              <div class="col auto">
                {this.renderStatus(active)}
              </div>
            </div>
          </h4>
          <div class="row">
            <div class="col xs-9 sm9 mt8">
              <div>{cfg.description}</div>
              <div>{cfg.servers ? cfg.servers.length : 0} servers</div>
            </div>
            <div class="col auto mt8">
              {this.renderControls(active, cfg)}
            </div>
          </div>
        </div>
      </div>
    )
  }

  public render() {
    const {configs} = this.state
    return (
      <div>
        <div class="row align-center">
          <div class="col xs-11">
            <h1>Configs</h1>
          </div>
          <div class="col auto txr">
            <a href={uiConfigEditFmt("new")}>
              <A4IcnAdd maxHeight={32} />
            </a>
          </div>
        </div>
        <div class="row">
          <div class="col auto">
            {configs ? (
              <div>
                <div class="col auto sm-down-hide">
                  <div class="frame">
                    {this.renderTable(configs)}
                  </div>
                </div>
                <div class="col auto md-up-hide">
                  {configs.map(cfg => this.renderCard(cfg))}
                </div>
              </div>
            ) : []}
          </div>
        </div>
      </div>
    )
  }

}

export default (props: A4CProps) => <A4Configs s={useContext(A4Context)} />
