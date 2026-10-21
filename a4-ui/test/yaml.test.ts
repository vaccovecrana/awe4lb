import {test} from "node:test"
import assert from "node:assert/strict"
import {
  emptyDiscover,
  emptyHealth,
  emptyTls,
  fromConfig,
  newConfigForm,
  parseConfigYaml,
  toConfig,
  yamlOf
} from "../@a4ui/yaml.ts"

test("new configuration form round-trips through YAML", () => {
  const form = newConfigForm()
  form.id = "edge"
  form.description = "Edge configuration"
  const parsed = parseConfigYaml(yamlOf(form))
  assert.equal(parsed.error, undefined)
  assert.equal(parsed.form?.id, "edge")
  assert.equal(parsed.form?.description, "Edge configuration")
  assert.equal(parsed.form?.servers.length, 1)
  assert.equal(parsed.form?.servers[0].matches[0].hosts[0].host, "127.0.0.1")
})

test("reports YAML syntax errors with a line and column", () => {
  const parsed = parseConfigYaml("id: [unterminated\nservers: []\n")
  assert.ok(parsed.error)
  assert.match(parsed.error!, /line \d+/)
})

test("rejects non-mapping YAML documents", () => {
  assert.match(parseConfigYaml("- a\n- b\n").error!, /mapping/)
})

test("discovery replaces static backend hosts in the wire config", () => {
  const form = newConfigForm()
  form.id = "discover"
  form.description = "Discovery"
  const match = form.servers[0].matches[0]
  match.discover = {...emptyDiscover(), enabled: true, kind: "http", endpoint: "http://127.0.0.1:4010"}
  const cfg = toConfig(form)
  const wireMatch = cfg.servers![0].match![0]
  assert.equal(wireMatch.pool?.hosts, undefined)
  assert.equal(wireMatch.discover?.http?.endpoint, "http://127.0.0.1:4010")
})

test("weights and priorities are only emitted as a pair", () => {
  const form = newConfigForm()
  form.id = "weights"
  form.description = "Weights"
  const host = form.servers[0].matches[0].hosts[0]
  host.weight = "3"
  host.priority = ""
  assert.equal(toConfig(form).servers![0].match![0].pool!.hosts![0].weight, undefined)

  host.priority = "1"
  const pair = toConfig(form).servers![0].match![0].pool!.hosts![0]
  assert.equal(pair.weight, 3)
  assert.equal(pair.priority, 1)
})

test("maps a complete configuration into the form and back", () => {
  const cfg = {
    id: "full",
    description: "Full",
    servers: [{
      id: "https",
      addr: {host: "0.0.0.0", port: 8443},
      tls: {protocols: ["TLSv1.3"], ciphers: ["TLS_AES_128_GCM_SHA256"], base: {certPath: "a.pem", keyPath: "a.key"}},
      match: [{
        op: {sni: {endsWith: ".localhost"}},
        pool: {type: "weight", hosts: [{addr: {host: "10.0.0.1", port: 3000}, weight: 2, priority: 0}]},
        healthCheck: {intervalMs: 5000, timeoutMs: 2500, exec: {command: "nc", args: ["-z", "$host", "$port"]}},
        tls: {certPath: "b.pem", keyPath: "b.key", open: true}
      }]
    }]
  }
  const form = fromConfig(cfg as any)
  assert.equal(form.servers[0].transport, "tls")
  assert.equal(form.servers[0].matches[0].op.kind, "sni")
  assert.equal(form.servers[0].matches[0].op.op, "endsWith")
  assert.equal(form.servers[0].matches[0].health.args, "-z $host $port")

  const round = toConfig(form)
  assert.equal(round.servers![0].tls?.base?.certPath, "a.pem")
  assert.equal(round.servers![0].match![0].op?.sni?.endsWith, ".localhost")
  assert.equal(round.servers![0].match![0].pool?.type, "weight")
  assert.equal(round.servers![0].match![0].tls?.open, true)
})

test("maps transport exclusively and emits only the selected block", () => {
  const tls = fromConfig({
    id: "tls", description: "tls",
    servers: [{id: "s", addr: {host: "0.0.0.0", port: 8443}, tls: {}, match: [{}]}]
  } as any)
  assert.equal(tls.servers[0].transport, "tls")
  const tlsWire = toConfig(tls).servers![0]
  assert.ok(tlsWire.tls)
  assert.equal(tlsWire.udp, undefined)

  const udp = fromConfig({
    id: "udp", description: "udp",
    servers: [{id: "s", addr: {host: "0.0.0.0", port: 8070}, udp: {}, match: [{}]}]
  } as any)
  assert.equal(udp.servers[0].transport, "udp")
  const udpWire = toConfig(udp).servers![0]
  assert.ok(udpWire.udp)
  assert.equal(udpWire.tls, undefined)

  const tcp = fromConfig({
    id: "tcp", description: "tcp",
    servers: [{id: "s", addr: {host: "0.0.0.0", port: 8080}, match: [{}]}]
  } as any)
  assert.equal(tcp.servers[0].transport, "tcp")
})

test("UDP servers omit match-level backend TLS", () => {
  const form = fromConfig({
    id: "udp", description: "udp",
    servers: [{
      id: "s", addr: {host: "0.0.0.0", port: 8070}, udp: {},
      match: [{tls: {certPath: "a.pem", keyPath: "a.key"}}]
    }]
  } as any)
  form.servers[0].matches[0].tls = {enabled: true, certPath: "a.pem", keyPath: "a.key", open: false}
  const wire = toConfig(form).servers![0]
  assert.equal(wire.match![0].tls, undefined)
})

test("re-parsing YAML preserves stable form keys by position", () => {
  const form = newConfigForm()
  form.id = "stable"
  const serverKey = form.servers[0].key
  const matchKey = form.servers[0].matches[0].key
  const parsed = parseConfigYaml(yamlOf(form), form)
  assert.equal(parsed.form?.servers[0].key, serverKey)
  assert.equal(parsed.form?.servers[0].matches[0].key, matchKey)
})

test("empty optional sections are omitted from the wire config", () => {
  const form = fromConfig({
    id: "x",
    description: "x",
    servers: [{id: "tcp", addr: {host: "0.0.0.0", port: 8080}, match: [{}]}]
  } as any)
  const match = form.servers[0].matches[0]
  assert.equal(form.servers[0].transport, "tcp")
  assert.equal(match.discover.enabled, false)
  assert.equal(match.health.enabled, false)
  assert.equal(match.tls.enabled, false)
  assert.deepEqual(emptyTls(), match.tls)
  assert.deepEqual(emptyHealth(), match.health)
})
