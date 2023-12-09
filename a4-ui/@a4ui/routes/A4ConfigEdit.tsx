import * as React from "preact/compat"
import { useContext } from "preact/hooks"
import { stringify } from "yaml"

import { lockUi, A4Context, A4Store, usrInfo } from "@a4ui/store"
import { RenderableProps } from "preact"
import { apiV1ConfigGet } from "@a4ui/rpc"
import { A4Config } from "@a4ui/rpc"

type A4EProps = RenderableProps<{ s?: A4Store, configId: string }>
interface A4EState { config: A4Config }

class A4ConfigEdit extends React.Component<A4EProps, A4EState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigGet(this.props.configId))
      .then(config => this.setState({config}))
      .then(() => lockUi(false, d))
      .catch(err => {
        console.log(err)
        return usrInfo(`Invalid configuration id: [${this.props.configId}]`, d)
          .then(() => lockUi(false, d));
      })
  }

  public render() {
    return (
      <div class="p8">
        <h2>
          Edit configuration <code>{this.state.config ? this.state.config.id : ""}</code>
        </h2>
        {this.state.config ? (
          <div class="row">
            <div class="col auto">
              <textarea class="form-control" rows={32}>
                {stringify(this.state.config)}
              </textarea>
            </div>
          </div>
        ) : (
          <div class="row">
            <div class="col auto">
              <div class="card">
                <div class="card-body">
                  <div class="txc">No config</div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

}

export default (props: A4EProps) => <A4ConfigEdit s={useContext(A4Context)} {...props} />
