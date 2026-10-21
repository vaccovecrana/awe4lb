## Testing

The regular test run is Docker-free:

```shell
./gradlew :a4-test:test
```

It executes the unit and in-process proxy/statistics tests (including
`A4LbStatsTest`, which starts a local echo server and verifies TCP byte
counters, connection counts and the stats ring buffer).

## Docker integration suite

`A4ServiceTest` exercises real routing through the dummy backends in
`src/test/resources/docker-compose.yml`. It is opt-in and requires Docker:

```shell
./a4-test/e2e.sh
```

or, after starting compose manually:

```shell
A4_INTEGRATION_TESTS=1 ./gradlew :a4-test:test
./gradlew :a4-test:test -Pa4Integration
```

Useful overrides:

```shell
A4_TEST_CLASS=A4ServiceTest ./a4-test/e2e.sh
KEEP_COMPOSE=1 ./a4-test/e2e.sh
SHOW_LOG=1 ./a4-test/e2e.sh
```

Manual equivalent:

```shell
docker compose -f a4-test/src/test/resources/docker-compose.yml up -d --build
A4_INTEGRATION_TESTS=1 ./gradlew :a4-test:test
docker compose -f a4-test/src/test/resources/docker-compose.yml down -v
```

| Service group | Host ports | Purpose |
|---|---|---|
| `momo-discover-json/txt` | 4010, 4011 | HTTP discovery |
| `momo-00..02`, `sdr-00..02` | 4000–4002, 4020–4022 | HTTP/TLS backends |
| `momo-udp-00..02` | 6000–6002/udp | UDP echo backends |
| test proxies | 8080, 8090, 8443, 8070 | listeners opened by `test-config-00.json` |

`momo.localhost` and `sdr.localhost` must resolve to loopback. If they do not:

```text
127.0.0.1 momo.localhost sdr.localhost
```

> The Docker suite can run on macOS, but the networking setup is cumbersome;
> Linux is simpler.

Generate a self-signed SSL certificate for local subdomains:

```
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -addext "subjectAltName = DNS:*.localhost" \
  -keyout ./awe4lb.key -out ./awe4lb.pem
```

## Security checks

1. Generate UDP connections in large numbers. This should exercise the LB's capability to clear out expired UDP sessions (TTL timeout).
