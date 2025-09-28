# Configuration Format

`awe4lb` uses a JSON-based Domain-Specific Language to define load balancer behavior. Configurations are loaded via the `--config` flag or REST API (`http://localhost:7070/api/v1/config`).

Each configuration is a JSON object with an `id`, optional `description`, and a `servers` array defining load balancing rules.

## Structure

### Root object

- `id`: Unique identifier (e.g., `"test-config-00"`).
- `description`: Optional human-readable description.
- `servers`: Array of server definitions (see below).

### Server Definitions

Servers specify listening sockets (host/port) and rules for routing traffic. Each server has:

- `id`: Unique name (e.g., `"server-udp-echo"`).
- `addr`: Listening address (e.g., `{"host": "0.0.0.0", "port": 8070}`).
- `match`: Array of rules to select backend pools.

Optional configuration blocks can be defined for `tls` (TCP/TLS) or `udp` for UDP settings.

### Server Match Operations

Match rules (`match`) filter incoming connections based on conditions like SNI or host address, directing traffic to specific pools. Example: `{"op": {"sni": {"equals": "momo.localhost"}}}`.

### Static Host Pools

Pools (`pool`) define static backend hosts with addresses, weights, and priorities. For example:

```json
{"hosts": [{"addr": {"host": "127.0.0.1", "port": 6000}}]}
```

### Backend Host Pool Discovery Methods

Dynamic discovery populates pools via:

- **HTTP**: Fetches JSON/text from endpoints.
- **Exec**: Runs commands to retrieve hosts.
- **Kubernetes**: Queries K8s APIs for service endpoints.

### Health Checks

Health checks monitor backend availability:

- **TCP Ping**: Simple connection tests.
- **Exec**: Custom commands (e.g., `ping` or `nc`).

### TLS Termination

Configures TLS for servers:

- **Ciphers/Protocols**: Custom security settings.
- **Default Certs**: Base certificate/key for all matches.
- **SNI-Specific Certs**: Per-domain certificates.
- **Open Backend TLS**: Proxies to TLS-enabled backends.

### UDP Configuration

UDP servers support sticky sessions only, regardless of the backend selection method:

- **Buffer Size**: Configurable packet buffers.
- **Timeouts**: Session idle timeouts.

## Example

> Note: `awe4lb` stores configuration files as JSON, but the UI allows you to edit configurations in YAML format for easier readability. This guide will use YAML formatting.

See [test-config-00.json](https://raw.githubusercontent.com/vaccovecrana/awe4lb/refs/heads/main/a4-test/src/test/resources/test-config-00.json) for a full example:

```yaml
id: test-config-00
servers:
  - id: test-udp-echo
    addr: {host: 0.0.0.0, port: 8070}
    match:
      - pool:
          type: ipHash
          hosts:
            - addr: {host: 127.0.0.1, port: 6000}
    udp:
      bufferSize: 2048
      idleTimeoutMs: 2000
      maxSessions: 1024
```

Read on for detailed configuration guides.
