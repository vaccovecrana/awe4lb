# awe4lb

`awe4lb` is a layer 4 load balancer

## Configuration notes

- TCP buffer sizes are currently determined by the Operating System.
- UDP buffer sizes are application specific. Default is 16384 bytes.

## Security considerations

- Do not allow public access to the REST api, since it allows for full management. Expose it only within a trusted network perimeter.
- Most applications and use cases should work fine with the default TCP buffer size. However, applications which stream large amounts of data should make sure that the underlying hardware has enough memory capacity to handle backpressure from either clients or backends.

## Implementation items

### TCP

- [x] Schema modeling (configuration template).
- [x] SSL connection tracking.
- [ ] ACME certificate issuance/renewal. For now, use `certbot` to rotate certificates, then restart `awe4lb`.

### UDP

- [x] UDP backend selection strategies (weight, random, round robin).
- [x] Sticky session support.
- [ ] UDP Transparent proxying.

### Backend discovery

- [ ] DNS records.
- [x] Exec return value.
- [x] Kubernetes.
- [ ] Any others provided by `gobetween`.

### Monitoring

- [ ] Metrics capturing.
  - [x] Bytes sent/received (global).
  - [ ] Whichever other metrics `gobetween` exposes.
- [ ] Metrics access (prometheus endpoint).

### Admin functionality

- [x] REST API access for configuration changes.
- [x] UI implementation.
- [ ] Documentation/Usage notes/caveats.

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
