## Implementation items

- [ ] Thread-per-request connection tracking PoC. Non-SSL.
- [ ] Schema modeling (configuration template).
- [ ] SSL connection tracking.
- [ ] Metrics capturing.
  - [ ] Bytes sent/received (global).
  - [ ] Whichever other metrics `gobetween` exposes.
- [ ] Metrics access (prometheus endpoint).
- [ ] Backend discovery implementations.
  - [ ] DNS records.
  - [ ] Exec return value.
  - [ ] Any others provided by `gobetween`.
- [ ] REST API access for configuration changes.
- [ ] UI implementation.
- [ ] Documentation/Usage notes/caveats.
