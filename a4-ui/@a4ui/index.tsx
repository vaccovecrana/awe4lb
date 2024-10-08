import "nord-ui"
import "nord-ui/dist/dark-theme.css"

import "../res/ui-lock.css"
import "../res/main.scss"

import * as React from "preact/compat"
import * as ReactDOM from "preact/compat"
import { useReducer } from "preact/hooks"
import Router from 'preact-router'

import { A4MenuLeft, A4MenuTop, A4UiLock } from "@a4ui/components"
import { initialState, A4Context, A4Reducer } from "@a4ui/store"
import { A4ConfigEdit, A4Configs, A4Dashboard } from "@a4ui/routes"
import { uiConfigEdit, uiConfigList, uiRoot } from "@a4ui/util"

class A4Shell extends React.Component {
  public render() {
    const [state, dispatch] = useReducer(A4Reducer, initialState)
    return (
      <A4Context.Provider value={{state, dispatch}}>
        <A4UiLock>
          <div id="app">
            <div class="row">
              <div class="col md-2 lg-2 xl-2 sm-down-hide">
                <A4MenuLeft />
              </div>
              <div class="col xs-12 sm-12 md-12 md-up-hide">
                <A4MenuTop />
              </div>
              <div class="col auto">
                <div class="p16">
                  <Router>
                    <A4Dashboard path={uiRoot} />
                    <A4Configs path={uiConfigList} />
                    <A4ConfigEdit path={uiConfigEdit} configId="" />
                  </Router>
                </div>
              </div>
            </div>
          </div>
        </A4UiLock>
      </A4Context.Provider>
    )
  }
}

ReactDOM.render(<A4Shell />, document.getElementById("root"))
