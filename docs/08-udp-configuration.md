# UDP Configuration

`awe4lb` supports UDP proxying with configurable session management, defined in the `udp` field of a server object. UDP servers handle stateless traffic, using sticky sessions to maintain client-backend affinity.

## Structure

A `udp` object includes:

- `bufferSize`: Packet buffer size in bytes (default: 16384, e.g., `2048`). Adjust for application needs (e.g., smaller for low-latency, larger for high-throughput).
- `idleTimeoutMs`: Session inactivity timeout in milliseconds (e.g., `2000`).
- `maxSessions`: Maximum concurrent sessions (e.g., `1024`).

## Example

A UDP echo server with sticky sessions:

```yaml
---
id: momo-udp-echo
addr: {host: 0.0.0.0, port: 8070}
match:
- pool:
    type: ipHash
    hosts:
    - addr: {host: 127.0.0.1, port: 6000}
    - addr: {host: 127.0.0.1, port: 6001}
udp:
  bufferSize: 2048
  idleTimeoutMs: 2000
  maxSessions: 1024
```
