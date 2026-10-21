import {getCurrentUrl} from "preact-router"
import {useContext} from "preact/hooks"
import {uiConfigList, uiRoot, uiTraffic} from "@a4ui/util"
import {A4Context} from "@a4ui/store"
import {A4Logo} from "@a4ui/components/A4Icons"

const A4Header = (props: {path?: string}) => {
  const {state} = useContext(A4Context)
  const current = props.path ?? getCurrentUrl()
  const navClass = (path: string, match = path): string => {
    const active = match === uiRoot ? current === uiRoot : current.startsWith(match)
    return active ? "a4-pill active" : "a4-pill"
  }
  return (
    <header class="a4-header">
      <div class="a4-header-inner">
        <a href={uiRoot} class="a4-brand">
          <A4Logo maxHeight={28}/>
          <span>awe4lb <span class="a4-brand-sub">Layer 4</span></span>
        </a>
        <nav class="a4-nav">
          <a class={navClass(uiRoot)} href={uiRoot}>Status</a>
          <a class={navClass(uiConfigList, "/config")} href={uiConfigList}>Configs</a>
          <a class={navClass(uiTraffic)} href={uiTraffic}>Traffic</a>
          {state.activeConfigId && (
            <a class="a4-pill" href={uiRoot} title="Active configuration">
              {state.activeConfigId}
            </a>
          )}
        </nav>
      </div>
    </header>
  )
}

export default A4Header
