import {Context, createContext} from "preact"

export type ToastSeverity = "error" | "info"

export interface Toast {
  message: string
  severity: ToastSeverity
}

export interface A4UiState {
  uiLocked: boolean
  lastMessage: Toast | undefined
  activeConfigId: string | undefined
}

export type A4Dispatch = (action: A4Action) => void

export interface A4Store {
  state: A4UiState
  dispatch: A4Dispatch
}

export type A4Action =
  | { type: "lockUi", payload: boolean }
  | { type: "usrMsg", payload: Toast }
  | { type: "usrMsgClear" }
  | { type: "activeConfig", payload: string | undefined }

export const hit = (act: A4Action, d: A4Dispatch): Promise<void> => {
  d(act)
  return Promise.resolve()
}

export const lockUi = (locked: boolean, d: A4Dispatch) => hit({type: "lockUi", payload: locked}, d)
export const usrInfo = (message: string, d: A4Dispatch) =>
  hit({type: "usrMsg", payload: {message, severity: "info"}}, d)
export const usrError = (message: string, d: A4Dispatch) =>
  hit({type: "usrMsg", payload: {message, severity: "error"}}, d)
export const usrMsgClear = (d: A4Dispatch) => hit({type: "usrMsgClear"}, d)
export const setActiveConfig = (id: string | undefined, d: A4Dispatch) =>
  hit({type: "activeConfig", payload: id}, d)

/**
 * Deduplicating error reporter for recurring background polls. The returned
 * function toasts only when the failure message changes; a `null` clears the
 * last-seen message (call it on a successful tick) so a later identical failure
 * is surfaced again.
 */
export const dedupeError = (d: A4Dispatch) => {
  let last: string | undefined
  return (message: string | null): void => {
    if (message === null) {
      last = undefined
      return
    }
    if (message === last) return
    last = message
    usrError(message, d)
  }
}

export const A4Reducer = (state0: A4UiState, action: A4Action): A4UiState => {
  switch (action.type) {
    case "usrMsg":
      return {...state0, lastMessage: action.payload}
    case "usrMsgClear":
      return {...state0, lastMessage: undefined}
    case "lockUi":
      return {...state0, uiLocked: action.payload}
    case "activeConfig":
      return state0.activeConfigId === action.payload ? state0 : {...state0, activeConfigId: action.payload}
  }
}

export const initialState: A4UiState = {
  lastMessage: undefined,
  uiLocked: false,
  activeConfigId: undefined
}

export const A4Context: Context<A4Store> = createContext({
  state: initialState, dispatch: (_action: A4Action) => {
  }
})
