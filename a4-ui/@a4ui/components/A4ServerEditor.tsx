import {A4Field} from "@a4ui/components/A4Field"
import A4MatchEditor from "@a4ui/components/A4MatchEditor"
import A4TlsEditor from "@a4ui/components/A4TlsEditor"
import {emptyMatch, MatchEntry, ServerEntry} from "@a4ui/yaml"

const A4ServerEditor = (props: {
  server: ServerEntry
  index: number
  canRemove: boolean
  onChange: (next: ServerEntry) => void
  onRemove: () => void
}) => {
  const s = props.server
  const patch = (p: Partial<ServerEntry>) => props.onChange({...s, ...p})
  const patchMatch = (key: string, next: MatchEntry) =>
    patch({matches: s.matches.map(m => m.key === key ? next : m)})
  const removeMatch = (key: string) =>
    patch({matches: s.matches.filter(m => m.key !== key)})

  return (
    <div class="a4-panel a4-mb-4">
      <div class="a4-server-head">
        <div class="a4-server-title">
          <h3>Server {props.index + 1}</h3>
          <span class="a4-status a4-status-unknown">{s.udpEnabled ? "UDP" : s.tlsEnabled ? "TLS" : "TCP"}</span>
        </div>
        {props.canRemove && (
          <button class="a4-btn-danger" type="button" onClick={props.onRemove}>Remove server</button>
        )}
      </div>

      <div class="a4-grid-3">
        <A4Field label="Server id">
          <input
            class="a4-input"
            data-testid="server-id"
            placeholder="http"
            value={s.id}
            onInput={(e: any) => patch({id: e.target.value})}
          />
        </A4Field>
        <A4Field label="Listen host">
          <input
            class="a4-input"
            data-testid="server-host"
            placeholder="0.0.0.0"
            value={s.host}
            onInput={(e: any) => patch({host: e.target.value})}
          />
        </A4Field>
        <A4Field label="Listen port">
          <input
            class="a4-input"
            data-testid="server-port"
            inputMode="numeric"
            placeholder="8080"
            value={s.port}
            onInput={(e: any) => patch({port: e.target.value})}
          />
        </A4Field>
      </div>

      <div class="a4-row-start a4-mb-4">
        <label class="a4-check">
          <input
            type="checkbox"
            checked={s.tlsEnabled}
            onChange={(e: any) => patch({tlsEnabled: e.target.checked})}
          />
          TLS termination
        </label>
        <label class="a4-check">
          <input
            type="checkbox"
            checked={s.udpEnabled}
            onChange={(e: any) => patch({udpEnabled: e.target.checked})}
          />
          UDP server
        </label>
      </div>

      {s.tlsEnabled && (
        <div class="a4-subpanel">
          <div class="a4-subpanel-head">
            <span class="a4-subpanel-title">Server TLS</span>
          </div>
          <div class="a4-grid-2">
            <A4Field label="Protocols" hint="Comma separated. Leave blank for JSSE defaults.">
              <input
                class="a4-input"
                placeholder="TLSv1.3, TLSv1.2"
                value={s.tlsProtocols}
                onInput={(e: any) => patch({tlsProtocols: e.target.value})}
              />
            </A4Field>
            <A4Field label="Ciphers" hint="Comma separated. Leave blank for JSSE defaults.">
              <input
                class="a4-input"
                placeholder="TLS_AES_128_GCM_SHA256"
                value={s.tlsCiphers}
                onInput={(e: any) => patch({tlsCiphers: e.target.value})}
              />
            </A4Field>
          </div>
          <A4TlsEditor
            title="Base certificate"
            tls={s.tlsBase}
            onChange={tlsBase => patch({tlsBase})}
          />
        </div>
      )}

      {s.udpEnabled && (
        <div class="a4-subpanel">
          <div class="a4-subpanel-head">
            <span class="a4-subpanel-title">UDP</span>
          </div>
          <div class="a4-grid-3">
            <A4Field label="Buffer size (bytes)">
              <input
                class="a4-input"
                inputMode="numeric"
                value={s.udpBufferSize}
                onInput={(e: any) => patch({udpBufferSize: e.target.value})}
              />
            </A4Field>
            <A4Field label="Idle timeout (ms)">
              <input
                class="a4-input"
                inputMode="numeric"
                value={s.udpIdleTimeoutMs}
                onInput={(e: any) => patch({udpIdleTimeoutMs: e.target.value})}
              />
            </A4Field>
            <A4Field label="Max sessions">
              <input
                class="a4-input"
                inputMode="numeric"
                value={s.udpMaxSessions}
                onInput={(e: any) => patch({udpMaxSessions: e.target.value})}
              />
            </A4Field>
          </div>
        </div>
      )}

      <div class="a4-mt16">
        <div class="a4-subpanel-head">
          <span class="a4-subpanel-title">Request matches</span>
          <button
            class="a4-pill"
            data-testid="add-match"
            type="button"
            onClick={() => patch({matches: [...s.matches, emptyMatch()]})}
          >
            + Add match
          </button>
        </div>
        {s.matches.map((m, idx) => (
          <A4MatchEditor
            key={m.key}
            match={m}
            index={idx}
            canRemove={s.matches.length > 1}
            onChange={next => patchMatch(m.key, next)}
            onRemove={() => removeMatch(m.key)}
          />
        ))}
      </div>
    </div>
  )
}

export default A4ServerEditor
