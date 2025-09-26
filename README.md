# awe4lb

`awe4lb` is a lightweight, high-performance Layer 4 (TCP/UDP) load balancer written in Java.

It supports TLS termination, backend selection via various algorithms (e.g., round-robin, weighted, IP hash, least connections), dynamic discovery (HTTP, exec, Kubernetes), health checks, and UDP proxying. Configurations use a simple JSON-based DSL for defining servers, matching rules, pools, and discovery mechanisms.

It includes a web-based UI for management and an API for runtime operations.

## Purpose

Incoming TCP/UDP traffic gets distributed across backend servers to improve scalability, reliability, and performance. Key use cases include:

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
- **Management**: REST API for config CRUD/select, web UI for visualization.

## TODO

- **Metrics**: Basic RX/TX averages, connection tracking.

## Quick Start

Grab the [latest release](https://github.com/vaccovecrana/awe4lb/releases) or [docker image](https://github.com/vaccovecrana/awe4lb/pkgs/container/awe4lb)

Run the load balancer:
```
java -jar a4-core/build/libs/a4-core-<version>.jar --api-host=0.0.0.0 --config=./path/to/configs/directory
```

Open `http://localhost:7070` in a browser for config management.

For an example load balancing configuration, see [test-config-00](./a4-test/src/test/resources/test-config-00.json)

## Configuration notes

- TCP buffer sizes are currently determined by the Operating System.
- UDP buffer sizes are application specific. Default is 16384 bytes.

## Security considerations

- Do not allow public access to the REST api, since it allows for full management. Expose it only within a trusted network perimeter.
- Most applications and use cases should work fine with the default TCP buffer size. However, applications which stream large amounts of data should make sure that the underlying hardware has enough memory capacity to handle backpressure from either clients or backends.

## Development

Requires Gradle 8 or later.

Create a file with the following content at `~/.gsOrgConfig.json`:

```
{
  "orgId": "vacco-oss",
  "orgConfigUrl": "https://raw.githubusercontent.com/vaccovecrana/org-config/refs/heads/main/vacco-oss-java-21.json"
}
```

Then run:

```
gradle clean build
```

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

In memory of James Perry McCaffrey (March 27, 1958 – December 17, 2023).
