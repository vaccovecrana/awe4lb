import * as ReactDOM from "preact/compat"
import Router, {getCurrentUrl} from "preact-router"
import {useReducer, useState} from "preact/hooks"

import {A4Context, A4Reducer, initialState} from "@a4ui/store"
import {A4Header, A4Lock} from "@a4ui/components"
import {A4ConfigEdit, A4Configs, A4Dashboard, A4Traffic} from "@a4ui/routes"

const RouterAny: any = Router

const A4Shell = () => {
  const [state, dispatch] = useReducer(A4Reducer, initialState)
  const [path, setPath] = useState(getCurrentUrl())
  return (
    <A4Context.Provider value={{state, dispatch}}>
      <A4Lock>
        <div class="a4-shell">
          <A4Header path={path}/>
          <main class="a4-main">
            <RouterAny onChange={(e: any) => setPath(e.url)}>
              <A4Dashboard default/>
              <A4Configs path="/config/list"/>
              <A4ConfigEdit path="/config/:configId/edit"/>
              <A4Traffic path="/traffic"/>
            </RouterAny>
          </main>
        </div>
      </A4Lock>
    </A4Context.Provider>
  )
}

const app = document.getElementById("app")
if (app) {
  ReactDOM.render(<A4Shell/>, app)
}
