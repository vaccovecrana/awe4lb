import {useContext, useEffect, useRef, useState} from "preact/hooks"
import {A4Context, lockUi, usrError, usrInfo} from "@a4ui/store"
import {apiV1ConfigGet, apiV1ConfigPost, A4Validation} from "@a4ui/rpc"
import {messageOf} from "@a4ui/api"
import {uiConfigEditFmt, uiConfigList} from "@a4ui/util"
import {A4Field} from "@a4ui/components/A4Field"
import A4ServerEditor from "@a4ui/components/A4ServerEditor"
import {
  ConfigForm, emptyServer, fromConfig, newConfigForm, parseConfigYaml, ServerEntry, toConfig, yamlOf
} from "@a4ui/yaml"

const ID_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]*$/

const A4ConfigEdit = (props: {configId?: string, path?: string, default?: boolean}) => {
  const {dispatch} = useContext(A4Context)
  const isNew = !props.configId || props.configId === "new"
  const configId = isNew ? "" : props.configId || ""

  const [form, setForm] = useState<ConfigForm>(newConfigForm)
  const [yamlText, setYamlText] = useState("")
  const [yamlError, setYamlError] = useState("")
  const [yamlWarnings, setYamlWarnings] = useState<string[]>([])
  const [formErrors, setFormErrors] = useState<string[]>([])
  const [loadedId, setLoadedId] = useState("")
  const [loaded, setLoaded] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [dirty, setDirty] = useState(false)

  const load = () => {
    setLoaded(false)
    setFormErrors([])
    setYamlError("")
    setYamlWarnings([])
    lockUi(true, dispatch)
    if (isNew) {
      const next = newConfigForm()
      setForm(next)
      setYamlText(yamlOf(next))
      setLoadedId("new")
      setDirty(false)
      setLoaded(true)
      lockUi(false, dispatch)
      return
    }
    apiV1ConfigGet(configId)
      .then(cfg => {
        const next = fromConfig(cfg)
        setForm(next)
        setYamlText(yamlOf(next))
        setLoadedId(cfg.id || configId)
        setDirty(false)
      })
      .catch(e => usrError(messageOf(e), dispatch))
      .finally(() => {
        setLoaded(true)
        lockUi(false, dispatch)
      })
  }

  useEffect(load, [configId])

  const applyForm = (next: ConfigForm) => {
    setForm(next)
    setYamlText(yamlOf(next))
    setYamlError("")
    setYamlWarnings([])
    setFormErrors([])
    setDirty(true)
  }

  const patchServer = (key: string, next: ServerEntry) =>
    applyForm({...form, servers: form.servers.map(s => s.key === key ? next : s)})

  const removeServer = (key: string) =>
    applyForm({...form, servers: form.servers.filter(s => s.key !== key)})

  const addServer = () => applyForm({...form, servers: [...form.servers, emptyServer()]})

  const onYamlEdit = (text: string) => {
    setYamlText(text)
    setDirty(true)
    const parsed = parseConfigYaml(text, form)
    setYamlWarnings(parsed.warnings)
    if (parsed.error) {
      setYamlError(parsed.error)
      return
    }
    setYamlError("")
    if (parsed.form) {
      setForm(parsed.form)
    }
  }

  const formatYaml = () => {
    const text = yamlOf(form)
    setYamlText(text)
    setYamlError("")
    setYamlWarnings([])
  }

  const save = () => {
    if (yamlError) {
      usrError(`Fix the YAML error first: ${yamlError}`, dispatch)
      return
    }
    const errors: string[] = []
    if (!form.id.trim()) {
      errors.push("A configuration id is required.")
    } else if (isNew && !ID_PATTERN.test(form.id.trim())) {
      errors.push(`Invalid configuration id "${form.id}": use letters, numbers, dots, dashes or underscores.`)
    } else if (!isNew && form.id.trim() !== loadedId) {
      errors.push(`The configuration id cannot be changed from "${loadedId}".`)
    }
    if (form.servers.length === 0) {
      errors.push("Add at least one server.")
    }
    if (errors.length > 0) {
      setFormErrors(errors)
      return
    }
    setFormErrors([])
    setProcessing(true)
    lockUi(true, dispatch)
    apiV1ConfigPost(loadedId || configId, toConfig(form))
      .then((validations: A4Validation[]) => {
        if (validations && validations.length > 0) {
          setFormErrors(validations.map(v => v.message || v.key || "Invalid configuration"))
          return
        }
        setDirty(false)
        usrInfo("Configuration saved", dispatch)
        if (isNew) {
          window.setTimeout(() => window.location.replace(uiConfigEditFmt(form.id.trim())), 500)
        }
      })
      .catch(e => usrError(messageOf(e), dispatch))
      .finally(() => {
        setProcessing(false)
        lockUi(false, dispatch)
      })
  }

  const saveRef = useRef<() => void>(() => {})
  saveRef.current = save

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "s") {
        e.preventDefault()
        saveRef.current()
      }
    }
    window.addEventListener("keydown", handler)
    return () => window.removeEventListener("keydown", handler)
  }, [])

  useEffect(() => {
    if (!dirty) {
      return
    }
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ""
    }
    window.addEventListener("beforeunload", handler)
    return () => window.removeEventListener("beforeunload", handler)
  }, [dirty])

  if (!loaded) {
    return (
      <div>
        <div class="a4-skeleton" style="height:40px;width:320px"/>
        <div class="a4-skeleton a4-skeleton-card a4-mt16" style="height:520px"/>
      </div>
    )
  }

  return (
    <section>
      <div class="a4-hero-row">
        <div>
          <h1>{isNew ? "New configuration" : `Edit ${loadedId}`}</h1>
          <div class="a4-subtitle">
            The form and the YAML document are kept in sync. Use whichever is more convenient.
          </div>
        </div>
        <div class="a4-hero-actions" style="margin-left:auto">
          <a class="a4-pill" href={uiConfigList}>← Configurations</a>
          <button class="a4-pill a4-pill--accent" data-testid="save-config" type="button" disabled={processing} onClick={save}>
            {processing ? "Saving…" : "Save"}
          </button>
        </div>
      </div>

      {formErrors.length > 0 && (
        <div class="a4-error">
          <ul style="margin:0;padding-left:18px">
            {formErrors.map((err, i) => <li key={i}>{err}</li>)}
          </ul>
        </div>
      )}

      {yamlError && <div class="a4-error">YAML: {yamlError}</div>}
      {yamlWarnings.length > 0 && (
        <div class="a4-warn">
          {yamlWarnings.map((w, i) => <div key={i}>{w}</div>)}
        </div>
      )}

      <div class="a4-edit">
        <div>
          <div class="a4-panel">
            <h2 class="a4-section-title">Configuration</h2>
            <div class="a4-grid-2">
              <A4Field label="Id" hint={isNew ? "Letters, numbers, dots, dashes or underscores." : "The id cannot be changed."}>
                <input
                  class="a4-input"
                  data-testid="config-id"
                  placeholder="my-config"
                  disabled={!isNew}
                  value={form.id}
                  onInput={(e: any) => applyForm({...form, id: e.target.value})}
                />
              </A4Field>
              <A4Field label="Description">
                <input
                  class="a4-input"
                  data-testid="config-description"
                  placeholder="Primary edge configuration"
                  value={form.description}
                  onInput={(e: any) => applyForm({...form, description: e.target.value})}
                />
              </A4Field>
            </div>
          </div>

          <div class="a4-subpanel-head a4-mt16">
            <span class="a4-subpanel-title">Servers ({form.servers.length})</span>
            <button class="a4-pill" type="button" data-testid="add-server" onClick={addServer}>+ Add server</button>
          </div>

          {form.servers.map((srv, idx) => (
            <A4ServerEditor
              key={srv.key}
              server={srv}
              index={idx}
              canRemove={form.servers.length > 1}
              onChange={next => patchServer(srv.key, next)}
              onRemove={() => removeServer(srv.key)}
            />
          ))}

          {form.servers.length === 0 && (
            <div class="a4-empty">Add at least one server to save this configuration.</div>
          )}
        </div>

        <div>
          <div class="a4-panel">
            <div class="a4-chart-head">
              <div>
                <h2 class="a4-section-title" style="margin-bottom:4px">YAML</h2>
                <span class="a4-muted" style="font-size:12px">
                  {yamlError ? "Invalid document" : dirty ? "Unsaved changes" : "Saved"}
                </span>
              </div>
              <div class="a4-row-start">
                <button class="a4-pill" type="button" onClick={formatYaml}>Format</button>
                <button class="a4-pill" type="button" onClick={load}>Reload</button>
              </div>
            </div>
            <textarea
              class={`a4-yaml${yamlError ? " a4-yaml--error" : ""}`}
              data-testid="yaml-editor"
              spellcheck={false}
              value={yamlText}
              onInput={(e: any) => onYamlEdit(e.target.value)}
            />
            <div class="a4-form-text">
              Edits here immediately update the form when the YAML is valid. {isNew
                ? "Saving a new configuration creates it on the server."
                : "Use Save or Ctrl/Cmd+S to persist changes."}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default A4ConfigEdit
