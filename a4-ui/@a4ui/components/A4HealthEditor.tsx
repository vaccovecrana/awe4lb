import {A4Field} from "@a4ui/components/A4Field"
import {HealthEntry} from "@a4ui/yaml"

const A4HealthEditor = (props: {
  health: HealthEntry
  udp?: boolean
  onChange: (next: HealthEntry) => void
}) => {
  const h = props.health
  const patch = (p: Partial<HealthEntry>) => props.onChange({...h, ...p})

  return (
    <div class="a4-subpanel">
      <div class="a4-subpanel-head">
        <span class="a4-subpanel-title">Health check</span>
        <label class="a4-check">
          <input
            type="checkbox"
            checked={h.enabled}
            onChange={(e: any) => patch({enabled: e.target.checked})}
          />
          Enabled
        </label>
      </div>
      {h.enabled && (
        <div>
          {props.udp && !h.command.trim() && (
            <div class="a4-form-text" style="color:var(--a4-nord13)">
              UDP health checks require an exec command.
            </div>
          )}
          <div class="a4-grid-2">
            <A4Field label="Interval (ms)" hint="Default: 5000">
              <input
                class="a4-input"
                inputMode="numeric"
                placeholder="5000"
                value={h.intervalMs}
                onInput={(e: any) => patch({intervalMs: e.target.value})}
              />
            </A4Field>
            <A4Field label="Timeout (ms)" hint="Default: 2500">
              <input
                class="a4-input"
                inputMode="numeric"
                placeholder="2500"
                value={h.timeoutMs}
                onInput={(e: any) => patch({timeoutMs: e.target.value})}
              />
            </A4Field>
          </div>
          <A4Field label="Command" hint="Optional. Without a command the backend is assumed reachable.">
            <input
              class="a4-input"
              placeholder="nc"
              value={h.command}
              onInput={(e: any) => patch({command: e.target.value})}
            />
          </A4Field>
          <A4Field label="Arguments" hint="Use $host and $port placeholders.">
            <input
              class="a4-input"
              placeholder="-z -v $host $port"
              value={h.args}
              onInput={(e: any) => patch({args: e.target.value})}
            />
          </A4Field>
        </div>
      )}
    </div>
  )
}

export default A4HealthEditor
