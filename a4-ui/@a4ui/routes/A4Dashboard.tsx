import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { A4Config, getInstances } from "@a4ui/rpc"
import { RenderableProps } from "preact"

type A4DProps = RenderableProps<{ s?: A4Store }>
interface A4DState { instances?: A4Config[] }

class A4Dashboard extends React.Component<A4DProps, A4DState> {

  constructor() {
    super()
  }

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      .then(() => getInstances())
      .then(instances => this.setState({instances}))
      .then(() => lockUi(false, d))
  }

  public render() {
    return this.state.instances ? (
      <div class="p8">
        <div class="row">
          {this.state.instances.map((lb) => (
            <div class="col md-2">
              <div class="p8 txc">
                {lb.id} - {lb.description}
              </div>
            </div>
          ))}
        </div>
      </div>
    ) : (<div />)
  }

}

export default (props: A4DProps) => <A4Dashboard s={useContext(A4Context)} />
