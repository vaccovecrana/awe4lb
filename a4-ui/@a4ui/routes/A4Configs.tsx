import {useContext, useEffect, useState} from "preact/hooks"
import {A4Context, lockUi, setActiveConfig, usrError, usrInfo} from "@a4ui/store"
import {A4Config, apiV1ConfigDelete, apiV1ConfigGet, apiV1ConfigListGet, apiV1ConfigSelectGet} from "@a4ui/rpc"
import {messageOf} from "@a4ui/api"
import {uiConfigEditFmt, uiRoot} from "@a4ui/util"
import {A4IcnAdd, A4IcnPlay, A4IcnStop, A4IconEdit, A4Inspect} from "@a4ui/components/A4Icons"
import A4StatusPill from "@a4ui/components/A4StatusPill"

const A4Configs = (props: { path?: string }) => {
  const {dispatch} = useContext(A4Context)
  const [active, setActive] = useState<A4Config>()
  const [configs, setConfigs] = useState<A4Config[]>([])
  const [loaded, setLoaded] = useState(false)

  const load = () => {
    lockUi(true, dispatch)
    Promise.all([apiV1ConfigGet(""), apiV1ConfigListGet()])
      .then(([activeCfg, list]) => {
        setActive(activeCfg?.id ? activeCfg : undefined)
        setConfigs(list || [])
        setActiveConfig(activeCfg?.id, dispatch)
      })
      .catch(e => usrError(messageOf(e), dispatch))
      .finally(() => {
        setLoaded(true)
        lockUi(false, dispatch)
      })
  }

  useEffect(load, [])

  const toggleActive = (cfg: A4Config) => {
    const isActive = active?.id === cfg.id
    lockUi(true, dispatch)
    apiV1ConfigSelectGet(isActive ? "" : cfg.id || "")
      .then(state => {
        setActive(state.active)
        setActiveConfig(state.active?.id, dispatch)
        return usrInfo(isActive ? "Configuration stopped" : `Configuration ${state.active?.id} activated`, dispatch)
      })
      .catch(e => usrError(messageOf(e), dispatch))
      .finally(() => lockUi(false, dispatch))
  }

  const remove = (cfg: A4Config) => {
    if (!cfg.id || !window.confirm(`Delete configuration "${cfg.id}"?`)) {
      return
    }
    lockUi(true, dispatch)
    apiV1ConfigDelete(cfg.id)
      .then(ok => {
        if (!ok) {
          return usrError("The configuration could not be deleted", dispatch)
        }
        return usrInfo("Configuration deleted", dispatch)
      })
      .then(load)
      .catch(e => usrError(messageOf(e), dispatch))
      .finally(() => lockUi(false, dispatch))
  }

  return (
    <section>
      <div class="a4-hero-row">
        <div>
          <h1>Configurations</h1>
          <div class="a4-subtitle">Load balancer definitions stored on the server.</div>
        </div>
        <div style="margin-left:auto">
          <a class="a4-pill a4-pill--accent" data-testid="new-config" href={uiConfigEditFmt("new")}>
            <A4IcnAdd maxHeight={14}/> New configuration
          </a>
        </div>
      </div>

      {!loaded && (
        <div class="a4-grid a4-mt16">
          <div class="a4-skeleton a4-skeleton-card"/>
          <div class="a4-skeleton a4-skeleton-card"/>
        </div>
      )}

      {loaded && configs.length === 0 && (
        <div class="a4-empty a4-mt16">No configurations yet. Create one to get started.</div>
      )}

      {loaded && configs.length > 0 && (
        <div class="a4-grid a4-mt16">
          {configs.map(cfg => {
            const isActive = active?.id === cfg.id
            return (
              <div class="a4-card" key={cfg.id} data-testid="config-card" data-config-id={cfg.id}>
                <div class="a4-card-body">
                  <div class="a4-row">
                    <a class="a4-card-title" href={isActive ? uiRoot : uiConfigEditFmt(cfg.id || "")}>{cfg.id}</a>
                    <A4StatusPill state={isActive ? "Active" : "Stopped"}/>
                  </div>
                  <div class="a4-card-sub">{cfg.description || "No description"}</div>
                  <div class="a4-card-meta">
                    {(cfg.servers || []).length} server{(cfg.servers || []).length === 1 ? "" : "s"}
                  </div>
                  <div class="a4-card-foot">
                    <div class="a4-card-actions">
                      <a class="a4-pill" href={uiConfigEditFmt(cfg.id || "")}>
                        <A4IconEdit maxHeight={14}/> Edit
                      </a>
                      <button class="a4-pill" data-action="toggle-active" type="button"
                              onClick={() => toggleActive(cfg)}>
                        {isActive ? <A4IcnStop maxHeight={14}/> : <A4IcnPlay maxHeight={14}/>}
                        {isActive ? "Stop" : "Activate"}
                      </button>
                      {isActive && (
                        <a class="a4-pill" href={uiRoot}><A4Inspect maxHeight={14}/> Inspect</a>
                      )}
                    </div>
                    <button class="a4-btn-danger" data-action="delete" type="button" onClick={() => remove(cfg)}>
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default A4Configs
