import {useContext, useEffect, useMemo, useState} from "preact/hooks"
import {
  CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale, LineElement, PointElement, Tooltip
} from "chart.js"
import {Line} from "react-chartjs-2"
import {A4Context, dedupeError, setActiveConfig} from "@a4ui/store"
import {apiV1StatsGet, A4Stats} from "@a4ui/rpc"
import {messageOf} from "@a4ui/api"
import {formatBytes, formatNumber, formatRate, formatTime, statusClass} from "@a4ui/util"

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend)

const RX_COLOR = "#88c0d0"
const TX_COLOR = "#d08770"

const A4Traffic = (props: {path?: string}) => {
  const {dispatch} = useContext(A4Context)
  const [stats, setStats] = useState<A4Stats>()
  const [selected, setSelected] = useState("all")
  const [paused, setPaused] = useState(false)

  useEffect(() => {
    let disposed = false
    const report = dedupeError(dispatch)
    const tick = () => {
      apiV1StatsGet()
        .then(next => {
          if (disposed) return
          setStats(next)
          setActiveConfig(next.configId, dispatch)
          report(null)
        })
        .catch(e => {
          if (disposed) return
          report(messageOf(e))
        })
    }
    if (!paused) {
      tick()
    }
    const timer = paused ? undefined : window.setInterval(tick, 2000)
    return () => {
      disposed = true
      if (timer !== undefined) {
        window.clearInterval(timer)
      }
    }
  }, [dispatch, paused])

  const backends = stats?.backends || []
  const effectiveSelected = selected === "all" || backends.some(b => b.id === selected)
    ? selected
    : "all"
  const history = stats?.history
  const timestamps = history?.timestamps || []
  const pointCount = timestamps.length

  const chartData = useMemo(() => {
    const rx = new Array(pointCount).fill(0)
    const tx = new Array(pointCount).fill(0)
    const series = history?.series || []
    if (effectiveSelected === "all") {
      for (const s of series) {
        for (let i = 0; i < pointCount; i++) {
          rx[i] += s.rxBps?.[i] || 0
          tx[i] += s.txBps?.[i] || 0
        }
      }
    } else {
      const s = series.find(x => x.id === effectiveSelected)
      if (s) {
        for (let i = 0; i < pointCount; i++) {
          rx[i] = s.rxBps?.[i] || 0
          tx[i] = s.txBps?.[i] || 0
        }
      }
    }
    return {
      labels: timestamps.map(t => formatTime(t)),
      datasets: [
        {
          label: "RX (backend → client)",
          data: rx,
          borderColor: RX_COLOR,
          backgroundColor: "rgba(136, 192, 208, 0.16)",
          pointRadius: 0,
          borderWidth: 2,
          tension: 0.25,
          fill: true,
        },
        {
          label: "TX (client → backend, mirrored)",
          data: tx.map(v => -v),
          borderColor: TX_COLOR,
          backgroundColor: "rgba(208, 135, 112, 0.14)",
          pointRadius: 0,
          borderWidth: 2,
          borderDash: [6, 4],
          tension: 0.25,
          fill: false,
        }
      ]
    }
  }, [history, pointCount, effectiveSelected])

  const options = useMemo<any>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    interaction: {mode: "index", intersect: false},
    plugins: {
      legend: {display: false},
      tooltip: {
        callbacks: {
          label: (ctx: any) => `${ctx.dataset.label}: ${formatRate(Math.abs(ctx.parsed.y))}`
        }
      }
    },
    scales: {
      x: {
        grid: {color: "rgba(76, 86, 106, 0.35)"},
        ticks: {color: "#7b88a1", maxTicksLimit: 8}
      },
      y: {
        beginAtZero: true,
        grid: {color: "rgba(76, 86, 106, 0.35)"},
        ticks: {color: "#7b88a1", callback: (value: any) => formatRate(Math.abs(Number(value)))}
      }
    }
  }), [])

  const totals = backends.reduce<{
    active: number
    connections: number
    rxBytes: number
    txBytes: number
    rxBps: number
    txBps: number
  }>((acc, b) => {
    acc.active += b.activeConnections || 0
    acc.connections += b.totalConnections || 0
    acc.rxBytes += b.rxBytes || 0
    acc.txBytes += b.txBytes || 0
    acc.rxBps += b.rxBps || 0
    acc.txBps += b.txBps || 0
    return acc
  }, {active: 0, connections: 0, rxBytes: 0, txBytes: 0, rxBps: 0, txBps: 0})

  return (
    <section>
      <div class="a4-hero-row">
        <div>
          <h1>Traffic</h1>
          <div class="a4-subtitle">
            Live backend counters sampled by the load balancer every second, with a five-minute ring buffer.
          </div>
        </div>
        <div class="a4-hero-actions" style="margin-left:auto">
          <span class="a4-muted" style="font-size:12px">
            {stats?.timestampMs ? `Updated ${formatTime(stats.timestampMs)}` : "Waiting for samples…"}
          </span>
          <button
            class={paused ? "a4-pill a4-pill--accent" : "a4-pill"}
            data-testid="traffic-pause"
            type="button"
            onClick={() => setPaused(!paused)}
          >
            {paused ? "Resume" : "Pause"}
          </button>
        </div>
      </div>

      <div class="a4-stat-grid">
        <div class="a4-stat">
          <div class="a4-stat-label">Active connections</div>
          <div class="a4-stat-value">{formatNumber(totals.active)}</div>
          <div class="a4-stat-sub">{formatNumber(totals.connections)} total accepted</div>
        </div>
        <div class="a4-stat">
          <div class="a4-stat-label">Throughput</div>
          <div class="a4-stat-value">{formatRate(totals.rxBps + totals.txBps)}</div>
          <div class="a4-stat-sub">RX {formatRate(totals.rxBps)} · TX {formatRate(totals.txBps)}</div>
        </div>
        <div class="a4-stat">
          <div class="a4-stat-label">RX total</div>
          <div class="a4-stat-value">{formatBytes(totals.rxBytes)}</div>
          <div class="a4-stat-sub">Backend → client</div>
        </div>
        <div class="a4-stat">
          <div class="a4-stat-label">TX total</div>
          <div class="a4-stat-value">{formatBytes(totals.txBytes)}</div>
          <div class="a4-stat-sub">Client → backend</div>
        </div>
      </div>

      <div class="a4-chart-panel">
        <div class="a4-chart-head">
          <div>
            <h2 class="a4-section-title" style="margin-bottom:4px">Throughput</h2>
            <div class="a4-legend">
              <span><i class="a4-legend-dot" style="background:#88c0d0"/>RX (backend → client)</span>
              <span><i class="a4-legend-dot" style="background:#d08770"/>TX (client → backend, mirrored)</span>
            </div>
          </div>
          <select
            class="a4-select"
            style="max-width:360px"
            value={effectiveSelected}
            onChange={(e: any) => setSelected(e.target.value)}
          >
            <option value="all">All backends</option>
            {backends.map(b => (
              <option value={b.id} key={b.id}>
                {b.serverId} · {b.matchLabel} · {b.host}:{b.port}
              </option>
            ))}
          </select>
        </div>
        {pointCount === 0 ? (
          <div class="a4-empty">No traffic samples yet. Activate a configuration and send traffic through it.</div>
        ) : (
          <div class="a4-chart">
            <Line data={chartData} options={options}/>
          </div>
        )}
      </div>

      <h2 class="a4-section-title">Backends</h2>
      {backends.length === 0 ? (
        <div class="a4-empty">No backend counters are available for the active configuration.</div>
      ) : (
        <>
          <div class="a4-table-wrap">
            <table class="a4-table">
              <thead>
                <tr>
                  <th>Server</th>
                  <th>Match</th>
                  <th>Backend</th>
                  <th>State</th>
                  <th class="a4-num">Active</th>
                  <th class="a4-num">Total conn.</th>
                  <th class="a4-num">RX</th>
                  <th class="a4-num">TX</th>
                  <th class="a4-num">RX/s</th>
                  <th class="a4-num">TX/s</th>
                </tr>
              </thead>
              <tbody>
                {[...backends].sort((a, b) =>
                  `${a.serverId}${a.matchLabel}${a.host}:${a.port}`.localeCompare(`${b.serverId}${b.matchLabel}${b.host}:${b.port}`)
                ).map(b => (
                  <tr key={b.id}>
                    <td>{b.serverId} <span class="a4-muted">({b.serverType})</span></td>
                    <td><code>{b.matchLabel}</code> <span class="a4-muted">· {b.poolType}</span></td>
                    <td class="a4-mono">{b.host}:{b.port}</td>
                    <td><span class={statusClass(b.state)}>{b.state || "Unknown"}</span></td>
                    <td class="a4-num">{formatNumber(b.activeConnections)}</td>
                    <td class="a4-num">{formatNumber(b.totalConnections)}</td>
                    <td class="a4-num">{formatBytes(b.rxBytes)}</td>
                    <td class="a4-num">{formatBytes(b.txBytes)}</td>
                    <td class="a4-num">{formatRate(b.rxBps)}</td>
                    <td class="a4-num">{formatRate(b.txBps)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div class="a4-table-hint">Scroll horizontally for connection and rate columns.</div>
        </>
      )}
    </section>
  )
}

export default A4Traffic
