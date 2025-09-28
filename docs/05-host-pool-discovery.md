# Backend Host Pool Discovery Methods

`awe4lb` supports dynamic discovery of backend hosts, populating `pool` objects via external sources. 

Defined in the `discover` field of a match rule, discovery methods fetch hosts from HTTP endpoints, exec commands, or Kubernetes APIs, ideal for dynamic environments.

## Structure

A `discover` object specifies one method:

- `http`: Fetches hosts via HTTP (JSON or text).
- `exec`: Runs a command to retrieve hosts.
- `k8s`: Queries Kubernetes APIs for service endpoints.

### HTTP Call for JSON and Plain Text Formats

Fetches hosts from an HTTP endpoint.

- `endpoint`: URL (e.g., `"http://127.0.0.1:4010"`).
- `format`: `json` or `text`.

Example:

```yaml
discover:
  http:
    endpoint: http://127.0.0.1:4010
    format: json
```

- **JSON**: Expects an array of `{"host": "<ip>", "port": <number>}` objects.
- **Text**: Expects lines of `<ip> <port> <weight>? <priority>?` (e.g., `127.0.0.1 3000 2 1`).

### Exec Command Discovery

Runs a command to fetch hosts.

- `command`: Executable (e.g., `"cat"`).
- `args`: Arguments (e.g., `["./sdr-hosts.json"]`).
- `format`: `json` or `text`.

Example:

```yaml
discover:
  exec:
    command: cat
    args: [./src/test/resources/discover/sdr-hosts.json]
    format: json
```

> Note: the output of the command should return host records in the same format as the HTTP call method would return.

### Kubernetes Discovery

Queries a Kubernetes API server for service endpoints.

- `port`: Target service port (e.g., `8080`).
- `service`: Service name (e.g., `"momo"`).
- `namespace`: Kubernetes namespace (e.g., `"test"`).
- `tokenPath`: Path to API token (e.g., `"./config/token"`).
- `apiUri`: API endpoint (e.g., `"https://k8s.example.com:6443"`).

Example:

```yaml
discover:
  k8s:
    port: 8080
    service: momo
    namespace: test
    tokenPath: ./config/token
    apiUri: https://k8s.example.com:6443
```

## Usage

- **Dynamic Updates**: `awe4lb` periodically refreshes pools from discovery sources.
- **Security**: Ensure `tokenPath` and `apiUri` are secure for Kubernetes.
