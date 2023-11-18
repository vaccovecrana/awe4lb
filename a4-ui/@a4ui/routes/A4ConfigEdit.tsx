import * as React from "preact/compat"

import { useContext } from "preact/hooks"
import { lockUi, A4Context, A4Store } from "@a4ui/store"
import { RenderableProps } from "preact"

type A4EProps = RenderableProps<{ s?: A4Store, configId: string }>
interface A4EState { json: string }

class A4ConfigEdit extends React.Component<A4EProps, A4EState> {

  public componentDidMount(): void {
    const {dispatch: d} = this.props.s
    lockUi(true, d)
      // .then(() => apiV1ConfigListGet())
      // .then(configs => this.setState({configs}))
      .then(() => lockUi(false, d))
  }

  public render() {
    return <div>Config: {this.props.configId}</div>
  }

}

export default (props: A4EProps) => <A4ConfigEdit s={useContext(A4Context)} {...props} />
