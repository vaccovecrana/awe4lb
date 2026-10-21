import {A4Field} from "@a4ui/components/A4Field"
import A4HostsEditor from "@a4ui/components/A4HostsEditor"
import A4DiscoverEditor from "@a4ui/components/A4DiscoverEditor"
import A4HealthEditor from "@a4ui/components/A4HealthEditor"
import A4TlsEditor from "@a4ui/components/A4TlsEditor"
import {MatchEntry, PoolType, ServerTransport} from "@a4ui/yaml"

const A4MatchEditor = (props: {
  match: MatchEntry
  index: number
  transport: ServerTransport
  canRemove: boolean
  onChange: (next: MatchEntry) => void
  onRemove: () => void
}) => {
  const m = props.match
  const isUdp = props.transport === "udp"
  const patch = (p: Partial<MatchEntry>) => props.onChange({...m, ...p})

  return (
    <div class="a4-match">
      <div class="a4-subpanel-head">
        <span class="a4-subpanel-title">Match {props.index + 1}</span>
        {props.canRemove && (
          <button class="a4-btn-danger" type="button" onClick={props.onRemove}>Remove match</button>
        )}
      </div>
      <div class="a4-grid-3">
        <A4Field label="Match on">
          <select
            class="a4-select"
            value={m.op.kind}
            onChange={(e: any) => patch({op: {...m.op, kind: e.target.value}})}
          >
            <option value="any">Any request</option>
            <option value="host">Host</option>
            <option value="sni">TLS SNI</option>
          </select>
        </A4Field>
        {m.op.kind !== "any" && (
          <>
            <A4Field label="Operator">
              <select
                class="a4-select"
                value={m.op.op}
                onChange={(e: any) => patch({op: {...m.op, op: e.target.value}})}
              >
                <option value="equals">equals</option>
                <option value="startsWith">startsWith</option>
                <option value="endsWith">endsWith</option>
              </select>
            </A4Field>
            <A4Field label="Value">
              <input
                class="a4-input"
                placeholder={m.op.kind === "sni" ? "example.localhost" : "10.0.0."}
                value={m.op.value}
                onInput={(e: any) => patch({op: {...m.op, value: e.target.value}})}
              />
            </A4Field>
          </>
        )}
        <A4Field label="Balancing">
          <select
            class="a4-select"
            value={m.poolType}
            onChange={(e: any) => patch({poolType: e.target.value as PoolType})}
          >
            <option value="">random</option>
            <option value="roundRobin">roundRobin</option>
            <option value="leastConn" disabled={isUdp}>
              {isUdp ? "leastConn (UDP unsupported)" : "leastConn"}
            </option>
            <option value="ipHash">ipHash</option>
            <option value="weight">weight</option>
          </select>
        </A4Field>
      </div>
      {!m.discover.enabled && (
        <A4HostsEditor hosts={m.hosts} onChange={hosts => patch({hosts})}/>
      )}
      <A4DiscoverEditor discover={m.discover} onChange={discover => patch({discover})}/>
      <A4HealthEditor health={m.health} udp={isUdp} onChange={health => patch({health})}/>
      {!isUdp && (
        <A4TlsEditor title="Backend TLS" tls={m.tls} onChange={tls => patch({tls})}/>
      )}
    </div>
  )
}

export default A4MatchEditor
