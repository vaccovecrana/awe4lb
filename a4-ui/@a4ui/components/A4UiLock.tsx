import * as React from "preact/compat"
import {RenderableProps} from "preact"

import { useContext } from "preact/hooks"
import { A4Context, usrMsgClear } from "@a4ui/store"

const A4UiLock = (props: RenderableProps<{}>) => {
  const {dispatch: d, state} = useContext(A4Context)
  if (state.lastMessage) {
    alert(JSON.stringify(state.lastMessage, null, 2))
    usrMsgClear(d)
  }
  return (
    <div>
      {props.children}
      {state.uiLocked ? <div class="uiLock" /> : []}
    </div>
  )
}

export default A4UiLock
