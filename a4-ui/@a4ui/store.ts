import * as React from "preact/compat"
import { Context, createContext } from "preact"

export interface A4UiState {
  uiLocked: boolean
  lastMessage: any
}

export type A4Dispatch = (action: A4Action) => void

export interface A4Store {
  state: A4UiState
  dispatch: A4Dispatch
}

export type A4Action =
  | {type: "lockUi", payload: boolean}
  | {type: "usrMsg", payload: string}
  | {type: "usrMsgClear"}

export const hit = (act: A4Action, d: A4Dispatch): Promise<void> => {
  d(act)
  return Promise.resolve()
}

export const lockUi = (locked: boolean, d: A4Dispatch) => hit({type: "lockUi", payload: locked}, d)
export const usrInfo = (payload: string, d: A4Dispatch) => hit({type: "usrMsg", payload}, d)
export const usrError = (payload: string, d: A4Dispatch) => hit({type: "usrMsg", payload}, d)
export const usrMsgClear = (d: A4Dispatch) => hit({type: "usrMsgClear"}, d)

export const A4Reducer: React.Reducer<A4UiState, A4Action> = (state0: A4UiState, action: A4Action): A4UiState => {
  switch (action.type) {
    case "usrMsg": return {...state0, lastMessage: action.payload}
    case "usrMsgClear": return {...state0, lastMessage: undefined}
    case "lockUi": return {...state0, uiLocked: action.payload}
  }
}

export const initialState: A4UiState = {
  lastMessage: undefined,
  uiLocked: false
}

export const A4Context: Context<A4Store> = createContext({
  state: initialState, dispatch: () => {}
})
