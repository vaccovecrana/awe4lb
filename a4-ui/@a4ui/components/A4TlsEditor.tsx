import {A4Field} from "@a4ui/components/A4Field"
import {TlsEntry} from "@a4ui/yaml"

const A4TlsEditor = (props: {
  title: string
  tls: TlsEntry
  onChange: (next: TlsEntry) => void
}) => {
  const t = props.tls
  const patch = (p: Partial<TlsEntry>) => props.onChange({...t, ...p})

  return (
    <div class="a4-subpanel">
      <div class="a4-subpanel-head">
        <span class="a4-subpanel-title">{props.title}</span>
        <label class="a4-check">
          <input
            type="checkbox"
            checked={t.enabled}
            onChange={(e: any) => patch({enabled: e.target.checked})}
          />
          Enabled
        </label>
      </div>
      {t.enabled && (
        <div>
          <A4Field label="Certificate path">
            <input
              class="a4-input"
              placeholder="./certs/server.pem"
              value={t.certPath}
              onInput={(e: any) => patch({certPath: e.target.value})}
            />
          </A4Field>
          <A4Field label="Key path">
            <input
              class="a4-input"
              placeholder="./certs/server.key"
              value={t.keyPath}
              onInput={(e: any) => patch({keyPath: e.target.value})}
            />
          </A4Field>
          <label class="a4-check">
            <input
              type="checkbox"
              checked={t.open}
              onChange={(e: any) => patch({open: e.target.checked})}
            />
            Trust any backend certificate
          </label>
        </div>
      )}
    </div>
  )
}

export default A4TlsEditor
