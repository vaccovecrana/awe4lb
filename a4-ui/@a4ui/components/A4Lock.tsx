import {RenderableProps} from "preact"
import {useContext, useEffect, useRef} from "preact/hooks"
import {A4Context, usrMsgClear} from "@a4ui/store"

const AUTO_DISMISS_MS = 6000

const A4Lock = (props: RenderableProps<{}>) => {
  const {dispatch: d, state} = useContext(A4Context)
  const msg = state.lastMessage
  const timer = useRef<number | undefined>(undefined)

  useEffect(() => {
    window.clearTimeout(timer.current)
    if (msg && msg.severity === "info") {
      timer.current = window.setTimeout(() => usrMsgClear(d), AUTO_DISMISS_MS)
    }
    return () => window.clearTimeout(timer.current)
  }, [msg])

  return (
    <div>
      {props.children}
      {state.uiLocked && (
        <div class="a4-lock-overlay">
          <div class="a4-lock-spinner"/>
        </div>
      )}
      {msg && (
        <div class={`a4-toast a4-toast--${msg.severity}`} role="alert">
          <span class="a4-toast-msg">{msg.message}</span>
          <button class="a4-toast-close" aria-label="Close" onClick={() => usrMsgClear(d)}>
            ×
          </button>
        </div>
      )}
    </div>
  )
}

export default A4Lock
