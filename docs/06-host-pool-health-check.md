# Health Checks

Health checks monitor backend availability, updating the `state` (`Up`, `Down`, `Unknown`) of hosts in a `pool`. Defined in the `healthCheck` field of a match rule, they ensure traffic routes only to healthy backends.

## Structure

A `healthCheck` object specifies:

- `intervalMs`: Check frequency in milliseconds (e.g., `60000`).
- `timeoutMs`: Timeout for checks (e.g., `15000`).
- Type: Either TCP ping or exec command for custom health check logic.

### Simple TCP Ping

Attempts a TCP connection to the backend’s `host` and `port`.

Example:

```yaml
healthCheck:
  intervalMs: 60000
  timeoutMs: 15000
```

### Exec Command Health Check

Runs a command to verify backend health.

- `command`: Executable (e.g., `"ping"` or `"nc"`).
- `args`: Arguments, with `$host` and `$port` placeholders (e.g., `["-c", "1", "$host"]`).

Example:

```yaml
healthCheck:
  exec:
    command: ping
    args: ['-c', '1', $host]
  intervalMs: 60000
  timeoutMs: 15000
```

## Example

TCP and exec health checks:

```yaml
id: test-http
addr: {host: 0.0.0.0, port: 80}
match:
  - pool:
      hosts:
        - addr: {host: 172.16.4.58, port: 3000}
    healthCheck:
      intervalMs: 60000
      timeoutMs: 15000
  - pool:
      hosts:
        - addr: {host: 172.16.4.59, port: 3000}
    healthCheck:
      exec:
        command: nc
        args: ['-z', '-v', $host, $port]
      intervalMs: 60000
      timeoutMs: 15000
```

## Usage

- **TCP Ping**: Simple, low-overhead check for basic connectivity.
- **Exec**: Flexible for custom checks (e.g., `nc` for port probing or `curl` for HTTP).
- **Timeouts**: Set `timeoutMs` to avoid hanging on slow backends.
- **State Updates**: Hosts marked `Down` are skipped by balancing algorithms.
