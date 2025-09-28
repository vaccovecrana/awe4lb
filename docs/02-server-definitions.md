# Server Definitions

Server definitions specify the listening sockets and rules for routing incoming TCP or UDP traffic. Each server is a JSON object within the `servers` array of a configuration file, defining how `awe4lb` binds to network interfaces and directs traffic to backends.

## Structure

A server object includes:

- `id`: Unique identifier (e.g., `"test-udp-echo"`).
- `addr`: Listening address and port (e.g., `{"host": "0.0.0.0", "port": 8070}`).
- `match`: Array of match rules to select backend pools (see [Server Match Operations](#03-server-match-operations.md)).

Optional configurations:

- `tls`: TLS settings for TCP servers (see [TLS Termination](#07-tls-termination.md)).
- `udp`: UDP settings (e.g., `{"bufferSize": 2048, "idleTimeoutMs": 2000, "maxSessions": 1024}`) (see [UDP Configuration](#08-udp-configuration.md)).

## Example

A TCP server with TLS termination and a UDP server:

```yaml
servers:
  - id: https-txt
    addr:
      host: 0.0.0.0
      port: 8443
    match:
      - op:
          sni:
            equals: momo.localhost
        pool:
          hosts:
            - addr:
                host: 127.0.0.1
                port: 3000
    tls:
      ciphers:
        - TLS_AES_128_GCM_SHA256
      base:
        certPath: /path/to/awe4lb.pem
        keyPath: /path/to/awe4lb.key
  - id: momo-udp-echo
    addr:
      host: 0.0.0.0
      port: 8070
    match:
      - pool:
          type: ipHash
          hosts:
            - addr:
                host: 127.0.0.1
                port: 6000
    udp:
      bufferSize: 2048
      idleTimeoutMs: 2000
      maxSessions: 1024

```

## Usage

- **Binding**: Use `0.0.0.0` to listen on all interfaces or a specific IP for restricted access.
- **Ports**: Ensure the user running `awe4lb` has permissions for ports < 1024 (e.g., via `CAP_NET_BIND_SERVICE` in systemd).
- **Multiple Servers**: Define multiple servers for different protocols or ports in the same config.
