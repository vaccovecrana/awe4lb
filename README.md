# awe4lb

`awe4lb` is a lightweight, high-performance Layer 4 (TCP/UDP) load balancer written in Java.

![a4lb-02](./docs/img/a4lb-02.png)

It supports TLS termination, backend selection via various algorithms (e.g., round-robin, weighted, IP hash, least
connections), dynamic discovery (HTTP, exec, Kubernetes), health checks, and UDP proxying. Configurations use a simple
JSON-based DSL for defining servers, matching rules, pools, and discovery mechanisms.

It includes a web-based UI for management and an API for runtime operations.

## Purpose

Incoming TCP/UDP traffic gets distributed across backend servers to improve scalability, reliability, and performance.
Key use cases include:

- TLS-termination proxies for HTTP services.
- Load balancing for databases, APIs, or custom protocols.
- UDP forwarding (e.g., for echo servers or custom apps).
- Dynamic backend discovery in environments like Kubernetes.

It prioritizes simplicity, low overhead, and extensibility while handling production workloads.

## Features

- **Protocols**: TCP (with TLS), UDP.
- **Balancing Algorithms**: Random, weighted, round-robin, IP hash, least connections.
- **Discovery**: Static hosts, HTTP endpoints, external commands, Kubernetes API.
- **Health Checks**: Exec-based (e.g., ping, nc) with configurable intervals/timeouts.
- **Management**: REST API for config CRUD/select, web UI with a bidirectional YAML/form config editor.
- **Metrics**: Per-backend RX/TX totals, throughput rates, connection counts and a server-side five-minute history ring
  buffer.

## Quick Start

Grab the [latest release](https://github.com/vaccovecrana/awe4lb/releases)
or [docker image](https://github.com/vaccovecrana/awe4lb/pkgs/container/awe4lb)

Run the load balancer:

```shell
a4lb --api-host=0.0.0.0 --config=./path/to/configs/directory
```

Or from a Docker image:

```shell
docker run -d --name awe4lb \
  -p 7070:7070 -p 8070:8070/udp -p 443:8443 \
  -v $(pwd)/configs:/etc/a4lb \
  ghcr.io/vaccovecrana/awe4lb:1.0.0 \
  --log-level=info --config=/etc/a4lb
```

> Note: in both cases, the user executing `a4lb` must be able to open any ports that will be used for front-end load
> balancing (i.e. port `443/tcp` for TLS termination, port `22/tcp` for SSH proxying, or any other services you define).

Open `http://localhost:7070` in a browser for config management.

### Web UI

The Preact UI is bundled with rspack and provides three views:

- **Status**: the active configuration, its servers, matches, TLS/UDP settings, discovery, health checks and live
  backend state.
- **Configs**: create, edit, activate, stop and delete configurations. The editor keeps a structured form and a YAML
  document in sync in both directions, reports YAML syntax errors with line/column information, and surfaces backend
  validation errors inline. `Ctrl/Cmd+S` saves.
- **Traffic**: live per-backend RX/TX throughput charts and counters, rendered from a server-side ring buffer sampled
  once per second.

The REST API also exposes the raw snapshot at `GET /api/v1/stats`.

Using the UI, you can define load balancer configurations. In this example, we have 3 front servers. The first proxying
HTTP connections, the second terminating HTTPS connections to HTTP backends, and the third proxying UDP connections via
sticky sessions.

```yaml
id: test-config-00
description: Test configuration
servers:
  - id: test-http
    addr: {host: 0.0.0.0, port: 80}
    pool:
      hosts: [{addr: {host: 172.16.4.58, port: 3000}}]
    healthCheck: {intervalMs: 60000, timeoutMs: 15000}
  - id: test-https
    addr: {host: 0.0.0.0, port: 443}
    match:
      - op: {sni: {equals: test-svc0.localhost}}
        pool:
          hosts: [{addr: {host: 172.16.4.59, port: 3000}}]
      - op: {sni: {equals: test-svc1.localhost}}
        pool:
          hosts: [{addr: {host: 172.16.4.58, port: 3000}}]
        healthCheck:
          exec: {command: nc, args: ['-z', '-v', $host, $port]}
    tls:
      ciphers: [TLS_AES_128_GCM_SHA256]
      base:
        certPath: ./src/test/resources/certs/awe4lb.pem
        keyPath: ./src/test/resources/certs/awe4lb.key
  - id: test-udp-echo
    addr: {host: 0.0.0.0, port: 8070}
    match:
      - pool:
          type: ipHash
          hosts: [{addr: {host: 127.0.0.1, port: 6000}}]
        healthCheck:
          exec: {command: ping, args: ['-c', '1', $host]}
    udp:
      bufferSize: 2048
      idleTimeoutMs: 2000
      maxSessions: 1024
```

For in-depth configuration options, [read the docs](https://vaccovecrana.github.io/awe4lb).

For an in-depth example load balancing configuration,
see [test-config-00](./a4-test/src/test/resources/test-config-00.json).

## Configuration notes

- TCP buffer sizes are currently determined by the Operating System.
- UDP buffer sizes are application specific. Default is 16384 bytes.

## Security considerations

- Do not allow public access to the REST API, since it allows for full management. Expose it only within a trusted
  network perimeter.
- Most applications and use cases should work fine with the default TCP buffer size. However, applications which stream
  large amounts of data should make sure that the underlying hardware has enough memory capacity to handle backpressure
  from either clients or backends.

## Development

Use the Gradle wrapper (Gradle 9.6) and JDK 21+.

Then run:

```
./gradlew clean build
```

The UI toolchain (Node 22.17, rspack, Sass) is provisioned by the Gradle node plugin.

Tests:

```
./gradlew build                 # compiles everything, runs Java + UI unit tests
npm --prefix a4-ui run test:unit
./gradlew :a4-ui:testUi         # UI unit tests only
./gradlew :a4-test:test         # Java unit + proxy/stats tests
```

The docker-compose dependent Java suite is opt-in:

```
./a4-test/e2e.sh
```

Browser suites run against a live instance and are opt-in as well. The
convenience runner starts a throwaway API/UI instance in the background and
stops it afterwards:

```shell
./a4-ui/test/run-suite.sh e2e
./a4-ui/test/run-suite.sh visual
```

Or point the suites at an instance you already started:

```shell
# Terminal 1
a4lb --config=./configs --api-port=7070

# Terminal 2
A4_UI_URL=http://127.0.0.1:7070 ./gradlew :a4-ui:e2eTest
A4_UI_URL=http://127.0.0.1:7070 ./gradlew :a4-ui:visual
```

`e2eTest` runs the Puppeteer suite; `visual` captures desktop and mobile
screenshots into `a4-ui/build/test-artifacts/visual/`. Set `A4_CHROME` if the
browser is not on the default path.

## Similar projects

- https://www.envoyproxy.io/
- https://gost.run/
- https://github.com/yyyar/gobetween

## Resources

- https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names
- https://stackoverflow.com/questions/53323855/sslserversocket-and-certificate-setup
- https://stackoverflow.com/a/62263402/491160
- https://github.com/raell2/SSLAsynchronousSocketChannel/tree/master
- https://stackoverflow.com/questions/14225957/socket-vs-socketchannel
- https://bugs.openjdk.org/browse/JDK-8202625
- https://github.com/yyyar/gobetween/issues/335
- https://github.com/felipejfc/go-udp-echo-server

## FBC - INTRODUCTION TO PARANATURAL TOPICS

> Presentation 27.1
> Presented by: Dr. Casper Darling
> Research assistance provided by: Carla Vaughn, Hubert Tan

Hello there! I'm Dr. Casper Darling, head of research at the Federal Bureau of Control.

Now, listen closely, because what we have here with `awe4lb` is nothing short of remarkable.

It's a load balancer, yes, but think of it as a Control Point in the digital realm—routing TCP and UDP traffic through
sockets that act like our very own Thresholds!

These altered streams of data, infused with the energies of everyday networks, are balanced across backends with
algorithms as precise as binding an Object of Power.

We've got discovery methods pulling from HTTP, commands, even Kubernetes—containing the chaos, keeping it stable.

But remember, every connection could be an intrusion, a potential AWE waiting to unfold.

As for me, well... I've been exposed to something profound, a resonance that's changing everything. This is my final
presentation on the matter.

Use `awe4lb` wisely; the Bureau's depending on it.

---

In memory of James Perry McCaffrey (March 27, 1958 – December 17, 2023).
