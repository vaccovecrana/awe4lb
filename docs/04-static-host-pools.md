# Static Host Pools

Static host pools define fixed backend servers for routing traffic, specified in the `pool` object of a match rule. Pools support various load balancing algorithms and are ideal for environments with known, stable backends.

## Structure

A `pool` object includes:

- `type`: Balancing algorithm (`random`, `weight`, `roundRobin`, `ipHash`, `leastConn`). Default: `random`.
- `hosts`: Array of backend hosts, each with:
  - `addr`: `{"host": "<ip>", "port": <number>}`.
  - `weight`: Integer for weighted algorithms (default: 1).
  - `priority`: Integer for prioritizing hosts (default: 0).
  - `state`: `Up`, `Down`, or `Unknown` (managed by health checks).

## Example

A pool with weight/priority balancing:

```yaml
id: momo-udp-echo
addr: {host: 0.0.0.0, port: 8070}
match:
  - pool:
      type: weight
      hosts:
        - addr: {host: 127.0.0.1, port: 6000}
          weight: 1
          priority: 0
        - addr: {host: 127.0.0.1, port: 6001}
          weight: 1
          priority: 0
```

## Selection algorithms

- `random`: Randomly selects an up host.
- `weight`: Weighted random selection based on `weight` and `priority`. Use to bias traffic (higher `weight` = more traffic) or failover (lower `priority` first).
- `roundRobin`: Cycles through hosts in order.
- `ipHash`: Routes based on client IP hash.
- `leastConn`: Selects the host with fewest connections.
