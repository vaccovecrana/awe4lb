## Implementation items

### TCP

- [ ] Thread-per-request connection tracking PoC. Non-SSL.
- [ ] Schema modeling (configuration template).
- [ ] SSL connection tracking.

### UDP

- [ ] UDP backend selection strategies (weight, random, round robin).
- [ ] Sticky session support.
- [ ] Transparent proxying?

### Backend discovery

- [ ] DNS records.
- [ ] Exec return value.
- [ ] Any others provided by `gobetween`.

### Monitoring

- [ ] Metrics capturing.
  - [ ] Bytes sent/received (global).
  - [ ] Whichever other metrics `gobetween` exposes.
- [ ] Metrics access (prometheus endpoint).

### Admin functionality

- [ ] REST API access for configuration changes.
- [ ] UI implementation.
- [ ] Documentation/Usage notes/caveats.
