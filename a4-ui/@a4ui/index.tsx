import "nord-ui"
import "nord-ui/dist/dark-theme.css"
import "icono"

import "../res/ui-lock.css"
import "../res/main.scss"

import * as React from "preact/compat"
import * as ReactDOM from "preact/compat"
import { useReducer } from "preact/hooks"
import Router from 'preact-router'

import { A4MenuLeft, A4MenuTop, A4UiLock } from "@a4ui/components"
import { initialState, A4Context, A4Reducer } from "@a4ui/store"
import { A4Dashboard } from "@a4ui/routes"

class A4Shell extends React.Component {
  public render() {
    const [state, dispatch] = useReducer(A4Reducer, initialState)
    return (
      <A4Context.Provider value={{state, dispatch}}>
        <A4UiLock>
          <div id="app">
            <div class="row">
              <div class="col sm-1 lg-1 xl-1 sm-down-hide">
                <A4MenuLeft />
              </div>
              <div class="col xs-12 sm-12 md-12 md-up-hide">
                <A4MenuTop />
              </div>
              <div class="col xs-12 sm-12 md-11 lg-11 xl-11">
                <div id="appFrame">
                  <Router>
                    <A4Dashboard path="/" />
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
