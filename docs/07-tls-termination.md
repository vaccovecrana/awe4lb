# TLS Termination

`awe4lb` supports TLS termination for TCP servers, securing connections with configurable ciphers, protocols, and certificates. TLS settings are defined in the `tls` field of a server or match rule.

## Structure

A `tls` object includes:

- `ciphers`: Array of cipher suites (e.g., `["TLS_AES_128_GCM_SHA256"]`).
- `protocols`: Optional protocol versions (e.g., `"TLSv1.2,TLSv1.3"`).
- `base`: Default certificate/key for the server.
- Optional: Per-match `tls` for SNI-specific certificates.

### Ciphers and Protocol Configurations

Specify secure ciphers and protocols.

For a list of reference cipher values, see [JSSE Cipher Suite Names](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names)

Commonly used and supported protocols in Java 11 include:

- `TLSv1`: Transport Layer Security, version 1.0
- `TLSv1.1`: Transport Layer Security, version 1.1
- `TLSv1.2`: Transport Layer Security, version 1.2
- `TLSv1.3`: Transport Layer Security, version 1.3 (introduced in Java 11)
- `SSLv3`: Secure Sockets Layer, version 3.0 (insecure and should be avoided)

Example:

```yaml
tls:
  ciphers: [TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384]
  protocols: TLSv1.3
```

### Default TLS Certificate for All Server Matches

A `base` object defines the default certificate/key pair.

- `certPath`: Path to a PEM certificate (e.g., `"/path/to/awe4lb.pem"`).
- `keyPath`: Path to a private key (e.g., `"/path/to/awe4lb.key"`).

Example:

```yaml
tls:
  base:
    certPath: "/path/to/awe4lb.pem"
    keyPath: "/path/to/awe4lb.key"
```

### SNI-Specific TLS Certificate/Key Bindings

Match-specific `tls` overrides the server’s `base` for specific SNI values.

Example:

```yaml
match:
- op:
    sni:
      equals: momo.localhost
  tls:
    certPath: "/path/to/momo.pem"
    keyPath: "/path/to/momo.key"
  pool:
    hosts:
    - addr: {host: 127.0.0.1, port: 3000}
```

### Open Backend TLS

Enables TLS to backend proxies providing their own certificates.

- `open`: Boolean to enable (e.g., `true`).

Example:

```yaml
match:
- op:
    sni:
      equals: momo.localhost
  tls:
    open: true
  pool:
    hosts:
    - addr: {host: 127.0.0.1, port: 443}
```

## Example

A TLS-enabled server:

```yaml
id: https-txt
addr: {host: 0.0.0.0, port: 8443}
match:
- op: {sni: {equals: momo.localhost}}
  tls: {certPath: "/path/to/momo.pem", keyPath: "/path/to/momo.key"}
  pool:
    hosts: [{adrr: {host: 127.0.0.1, port: 3000}}]
- op: {sni: {equals: sdr.localhost}}
  tls:
    open: true
  pool:
    hosts: [{adrr: {host: 127.0.0.1, port: 443}}]
tls:
  ciphers: [TLS_AES_128_GCM_SHA256]
  base: {certPath: "/path/to/awe4lb.pem", keyPath: "/path/to/awe4lb.key"}
```

## Usage

- **Ciphers**: Use modern ciphers (e.g., AES-GCM) for security.
- **SNI**: Support multiple domains with per-match certificates.
- **Open TLS**: Ideal for backends with their own TLS (e.g., HTTPS servers).
- **File Paths**: Ensure certificate/key files are accessible to the `awe4lb` process.
