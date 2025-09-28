# Server Match Operations

Match operations (`match`) filter incoming connections based on conditions like SNI (Server Name Indication) or client host address, routing them to specific backend pools. Defined in the `match` array of a server object, each rule pairs an operation (`op`) with a pool or discovery method.

## Structure

A match rule includes:

- `op`: Conditions to match (e.g., `sni: {equals: momo.localhost}` or `host: {startsWith: '127.0'}`).
- `pool`: Static or dynamic backend pool (see [Static Host Pools](#static-host-pools) or [Backend Host Pool Discovery](#backend-host-pool-discovery)).
- Optional: `healthCheck`, `tls`, `discover` (detailed in later sections).

## Supported Operations

- **SNI**: Matches TLS SNI (e.g., `sni: {equals: example.com}`).
- **Host**: Matches client IP/hostname (e.g., `host: {endsWith: 'localhost'}`).
- **Operators**: `equals`, `startsWith`, `endsWith` for string comparison.

## Example

Route connections based on SNI or host:

```yaml
id: https-txt
addr: {host: 0.0.0.0, port: 8443}
match:
  - op:
      sni:
        equals: momo.localhost
    pool:
      hosts:
        - addr:
            host: 127.0.0.1
            port: 3000
  - op:
      host:
        startsWith: '127.0'
    pool:
      hosts:
        - addr:
            host: 172.16.4.58
            port: 3000

```

## Usage

- **SNI**: Ideal for HTTPS servers with multiple domains.
- **Host**: Useful for IP-based routing (e.g., internal vs. external clients).
- **Order**: Rules are evaluated in order; the first match wins.
- **Default**: If no `op` is specified, the rule applies to all connections.
