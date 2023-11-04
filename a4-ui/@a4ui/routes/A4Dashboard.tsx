import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, A4Server, apiV1ConfigGet } from "@a4ui/rpc"
import { RenderableProps } from "preact"
import { A4ServerCard } from "@a4ui/components"

type A4DProps = RenderableProps<{ s?: A4Store }>
interface A4DState { config?: A4Config }

class A4Dashboard extends React.Component<A4DProps, A4DState> {

  constructor() {
    super()
  }

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigGet())
      .then(config => this.setState({config}))
      .then(() => lockUi(false, d))
  }

  public sortServers(servers: A4Server[]) {
    return servers.sort((a, b) => a.id.localeCompare(b.id))
  }

  public render() {
    var {config: cfg} = this.state;
    return (
      <div class="p8">
        <h2>{cfg ? "Active Configuration" : "No active configuration"}</h2>
        {cfg ? (
          <div class="row">
            <div class="col auto">
              <div class="p8 card minimal">
                <div class="card-title-1 pv8">{cfg.id}</div>
                <small>{cfg.description}</small>
                {cfg.servers.length > 0 ? ([
                  <div class="row">
                    {this.sortServers(cfg.servers).map(srv => (<A4ServerCard srv={srv} />))}
                  </div>
                ]) : []}
              </div>
            </div>
          </div>
        ) : []}
      </div>
    );
  }

}

export default (props: A4DProps) => <A4Dashboard s={useContext(A4Context)} />
