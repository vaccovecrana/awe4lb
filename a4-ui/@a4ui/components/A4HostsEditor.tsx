import {emptyHost, HostEntry} from "@a4ui/yaml"

const A4HostsEditor = (props: {
  hosts: HostEntry[]
  onChange: (hosts: HostEntry[]) => void
}) => {
  const update = (idx: number, patch: Partial<HostEntry>) => {
    props.onChange(props.hosts.map((h, i) => i === idx ? {...h, ...patch} : h))
  }
  const remove = (idx: number) => {
    props.onChange(props.hosts.filter((_, i) => i !== idx))
  }
  const add = () => props.onChange([...props.hosts, emptyHost()])

  return (
    <div>
      <div class="a4-form-label">Backends</div>
      {props.hosts.length === 0 && (
        <div class="a4-form-text a4-mb-2">No static backends. Add one, or enable discovery below.</div>
      )}
      <div style="display:flex;flex-direction:column;gap:8px">
        {props.hosts.map((h, idx) => (
          <div class="a4-host-row" key={h.key}>
            <input
              class="a4-input a4-host-field"
              data-testid="host-host"
              placeholder="host"
              value={h.host}
              onInput={(e: any) => update(idx, {host: e.target.value})}
            />
            <input
              class="a4-input a4-port-field"
              data-testid="host-port"
              placeholder="port"
              inputMode="numeric"
              value={h.port}
              onInput={(e: any) => update(idx, {port: e.target.value})}
            />
            <input
              class="a4-input a4-weight-field"
              data-testid="host-weight"
              placeholder="weight"
              inputMode="numeric"
              value={h.weight}
              onInput={(e: any) => update(idx, {weight: e.target.value, priority: h.priority || "0"})}
            />
            <input
              class="a4-input a4-priority-field"
              data-testid="host-priority"
              placeholder="priority"
              inputMode="numeric"
              value={h.priority}
              onInput={(e: any) => update(idx, {priority: e.target.value, weight: h.weight || "1"})}
            />
            <button class="a4-btn-danger" type="button" onClick={() => remove(idx)}>Remove</button>
          </div>
        ))}
      </div>
      <button class="a4-pill a4-mt8" data-testid="add-backend" type="button" onClick={add}>+ Add backend</button>
    </div>
  )
}

export default A4HostsEditor
