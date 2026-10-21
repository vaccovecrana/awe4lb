import {A4Field} from "@a4ui/components/A4Field"
import {DiscoverEntry} from "@a4ui/yaml"

const A4DiscoverEditor = (props: {
  discover: DiscoverEntry
  onChange: (next: DiscoverEntry) => void
}) => {
  const d = props.discover
  const patch = (p: Partial<DiscoverEntry>) => props.onChange({...d, ...p})

  return (
    <div class="a4-subpanel">
      <div class="a4-subpanel-head">
        <span class="a4-subpanel-title">Discovery</span>
        <label class="a4-check">
          <input
            type="checkbox"
            checked={d.enabled}
            onChange={(e: any) => patch({enabled: e.target.checked})}
          />
          Enabled
        </label>
      </div>
      {d.enabled && (
        <div>
          <div class="a4-grid-3">
            <A4Field label="Source">
              <select
                class="a4-select"
                value={d.kind}
                onChange={(e: any) => patch({kind: e.target.value})}
              >
                <option value="http">HTTP</option>
                <option value="exec">Command</option>
                <option value="k8s">Kubernetes</option>
              </select>
            </A4Field>
            <A4Field label="Interval (ms)">
              <input
                class="a4-input"
                inputMode="numeric"
                placeholder="10000"
                value={d.intervalMs}
                onInput={(e: any) => patch({intervalMs: e.target.value})}
              />
            </A4Field>
            <A4Field label="Timeout (ms)">
              <input
                class="a4-input"
                inputMode="numeric"
                placeholder="9900"
                value={d.timeoutMs}
                onInput={(e: any) => patch({timeoutMs: e.target.value})}
              />
            </A4Field>
          </div>
          {d.kind === "http" && (
            <div class="a4-grid-2">
              <A4Field label="Endpoint">
                <input
                  class="a4-input"
                  placeholder="http://127.0.0.1:4010/hosts"
                  value={d.endpoint}
                  onInput={(e: any) => patch({endpoint: e.target.value})}
                />
              </A4Field>
              <A4Field label="Format">
                <select
                  class="a4-select"
                  value={d.format}
                  onChange={(e: any) => patch({format: e.target.value})}
                >
                  <option value="json">json</option>
                  <option value="text">text</option>
                </select>
              </A4Field>
            </div>
          )}
          {d.kind === "exec" && (
            <div>
              <A4Field label="Command">
                <input
                  class="a4-input"
                  placeholder="cat"
                  value={d.command}
                  onInput={(e: any) => patch({command: e.target.value})}
                />
              </A4Field>
              <A4Field label="Arguments">
                <input
                  class="a4-input"
                  placeholder="./hosts.json"
                  value={d.args}
                  onInput={(e: any) => patch({args: e.target.value})}
                />
              </A4Field>
              <A4Field label="Format">
                <select
                  class="a4-select"
                  value={d.format}
                  onChange={(e: any) => patch({format: e.target.value})}
                >
                  <option value="json">json</option>
                  <option value="text">text</option>
                </select>
              </A4Field>
            </div>
          )}
          {d.kind === "k8s" && (
            <div class="a4-grid-2">
              <A4Field label="API server">
                <input
                  class="a4-input"
                  placeholder="https://k8s.example.com:6443"
                  value={d.apiUri}
                  onInput={(e: any) => patch({apiUri: e.target.value})}
                />
              </A4Field>
              <A4Field label="Token path">
                <input
                  class="a4-input"
                  placeholder="/var/run/secrets/token"
                  value={d.tokenPath}
                  onInput={(e: any) => patch({tokenPath: e.target.value})}
                />
              </A4Field>
              <A4Field label="Namespace">
                <input
                  class="a4-input"
                  value={d.namespace}
                  onInput={(e: any) => patch({namespace: e.target.value})}
                />
              </A4Field>
              <A4Field label="Service">
                <input
                  class="a4-input"
                  value={d.service}
                  onInput={(e: any) => patch({service: e.target.value})}
                />
              </A4Field>
              <A4Field label="Port">
                <input
                  class="a4-input"
                  inputMode="numeric"
                  value={d.port}
                  onInput={(e: any) => patch({port: e.target.value})}
                />
              </A4Field>
            </div>
          )}
          <div class="a4-form-text">Discovery replaces static backends with the discovered host set.</div>
        </div>
      )}
    </div>
  )
}

export default A4DiscoverEditor
