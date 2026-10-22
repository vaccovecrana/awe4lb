# Traffic Statistics

`awe4lb` samples every configured backend once per second and keeps the last
five minutes of samples in a server-side ring buffer. The UI renders that
history directly; no browser-side history accumulation is required.

## Endpoint

```
GET /api/v1/stats
```

The response contains the current snapshot and the history series:

```json
{
  "timestampMs": 1792584988925,
  "configId": "edge",
  "backends": [
    {
      "id": "https-0|0|10.0.0.11:3000",
      "serverId": "https",
      "serverType": "TLS",
      "matchLabel": "sni equals(api.localhost)",
      "poolType": "roundRobin",
      "host": "10.0.0.11",
      "port": 3000,
      "state": "Up",
      "activeConnections": 4,
      "totalConnections": 1821,
      "rxBytes": 9482211,
      "txBytes": 3120044,
      "rxBps": 18221.5,
      "txBps": 6120.0
    }
  ],
  "history": {
    "timestamps": [1792584987000, 1792584988000, 1792584989000],
    "series": [
      {
        "id": "https-0|0|10.0.0.11:3000",
        "rxBps": [0, 17500.0, 18221.5],
        "txBps": [0, 5900.0, 6120.0]
      }
    ]
  }
}
```

### Direction semantics

Counters are relative to the backend:

- `rxBytes` / `rxBps`: bytes flowing **backend → client**.
- `txBytes` / `txBps`: bytes flowing **client → backend**.

`activeConnections` is the current number of proxied sessions assigned to the
backend. `totalConnections` is monotonic for the lifetime of the active
configuration. Switching configurations creates a fresh set of counters.

### Sampling and retention

- Sample interval: 1 second.
- Ring capacity: 300 samples (5 minutes).
- Rates are derived from cumulative counters, so a missed sample is still
  reflected correctly in the next rate.
- Discovery replaces a pool's host set; counters for backends that are no
  longer present are pruned on the next discovery update.

## Traffic view

Open `/traffic` in the UI to see:

- active connections, total connections, aggregate throughput and RX/TX totals;
- an RX/TX throughput chart with an "All backends" or per-backend selector;
  TX is mirrored below the zero axis so both directions stay visible when
  their rates are equal;
- a table with each backend's state, connection counts, byte totals and rates.

The view polls `/api/v1/stats` every two seconds and can be paused.
