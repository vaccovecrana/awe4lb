import * as React from "preact/compat"
import { useContext } from "preact/hooks"
import { stringify, parse } from "yaml"

import { lockUi, A4Context, A4Store, usrInfo } from "@a4ui/store"
import { RenderableProps } from "preact"
import { A4Validation, apiV1ConfigGet, apiV1ConfigPost } from "@a4ui/rpc"

type A4EProps = RenderableProps<{ s?: A4Store, configId: string }>
interface A4EState {
  configTxt: string
  configErrors: A4Validation[]
}

class A4ConfigEdit extends React.Component<A4EProps, A4EState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigGet(this.props.configId))
      .then(config => this.setState({configTxt: stringify(config)}))
      .then(() => lockUi(false, d))
      .catch(err => {
        console.log(err)
        return usrInfo(`Invalid configuration id: [${this.props.configId}]`, d)
          .then(() => lockUi(false, d))
      })
  }

  public onSave() {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => apiV1ConfigPost(this.props.configId, parse(this.state.configTxt)))
      .then(res => this.setState({...this.state, configErrors: res}, () => {
        if (this.state.configErrors.length === 0) {
          return usrInfo("Config saved!", d)
        }
      }))
      .then(() => lockUi(false, d))
      .catch(err => {
        this.setState({...this.state, configErrors: [{
          message: err.toString(), args: undefined, format: undefined, key: undefined, name: undefined
        }]}, () => {
          lockUi(false, d)
        })
      })
  }

  public onEdit(e: React.JSX.TargetedEvent<HTMLTextAreaElement, Event>) {
    const target = e.target as any
    this.setState({...this.state, configTxt: target.value})
  }

  public render() {
    return (
      <div class="p8">
        <div class="row align-center">
          <div class="col xs-6 sm-6 md-6 lg-6 xl-6">
            <h2>
              Edit configuration <code>{this.props.configId}</code>
            </h2>
          </div>
          <div class="col xs-6 sm-6 md-6 lg-6 xl-6 txr">
            <button class="btn primary" onClick={() => this.onSave()}>
              Save
            </button>
          </div>
        </div>
        {this.state.configErrors && this.state.configErrors.length > 0 ? (
          <div class="callout danger">
            <ul>
              {this.state.configErrors.map(err => <li>{err.message}</li>)}
            </ul>
          </div>
        ) : []}
        <div class="row">
          <div class="col auto">
            <textarea class="form-control" rows={32} onChange={(e) => this.onEdit(e)}>
              {this.state.configTxt || ""}
            </textarea>
          </div>
        </div>
      </div>
    )
  }

}

export default (props: A4EProps) => <A4ConfigEdit s={useContext(A4Context)} {...props} />
