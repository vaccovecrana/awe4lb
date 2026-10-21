import puppeteer from "puppeteer"
import {existsSync} from "node:fs"
import {mkdir} from "node:fs/promises"
import {fileURLToPath} from "node:url"
import net from "node:net"
import path from "node:path"

export const UI_URL = (process.env.A4_UI_URL || "http://127.0.0.1:7070").replace(/\/+$/, "")
const VISUALS = fileURLToPath(new URL("../build/test-artifacts/visual/", import.meta.url))

let browser

const request = (url, options = {}, timeoutMs = 8000) =>
  fetch(url, {...options, signal: AbortSignal.timeout(timeoutMs)})

const uiReachable = async () => {
  try {
    const res = await request(UI_URL + "/", {method: "GET"}, 2000)
    return res.ok
  } catch {
    return false
  }
}

export const requireUi = async () => {
  if (await uiReachable()) return true
  console.log(`E2E skipped: ${UI_URL} not reachable (set A4_UI_URL)`)
  return false
}

const apiUrl = (p) => UI_URL + "/api" + p

export const openPage = async () => {
  if (!browser) {
    const candidates = [
      process.env.A4_CHROME,
      process.env.PUPPETEER_EXECUTABLE_PATH,
      "/usr/bin/chromium",
      "/usr/bin/chromium-browser",
      "/usr/bin/google-chrome",
      "/usr/bin/google-chrome-stable",
    ]
    const executablePath = candidates.find((p) => p && existsSync(p))
    browser = await puppeteer.launch({
      headless: true,
      args: ["--no-sandbox", "--disable-setuid-sandbox"],
      ...(executablePath ? {executablePath} : {}),
    })
  }
  const page = await browser.newPage()
  await page.setViewport({width: 1440, height: 950})
  const errors = []
  page.on("console", (m) => {
    if (m.type() === "error") errors.push(`console: ${m.text()}`)
  })
  page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`))
  // Accept confirmation and beforeunload dialogs so navigation never stalls.
  page.on("dialog", (dialog) => {
    dialog.accept().catch(() => {
    })
  })
  page.errors = errors
  return page
}

export const closeBrowser = async () => {
  if (browser) {
    await browser.close()
    browser = undefined
  }
}

export const goto = async (page, urlPath) => {
  await page.goto(UI_URL + urlPath, {waitUntil: "networkidle0", timeout: 20000})
}

export const text = (page, sel) => page.$eval(sel, (el) => el.textContent.trim())

export const setInput = async (page, sel, value) => {
  await page.waitForSelector(sel, {timeout: 5000})
  await page.$eval(sel, (el, v) => {
    const proto = el instanceof HTMLTextAreaElement
      ? HTMLTextAreaElement.prototype
      : HTMLInputElement.prototype
    const setter = Object.getOwnPropertyDescriptor(proto, "value").set
    setter.call(el, v)
    el.dispatchEvent(new Event("input", {bubbles: true}))
  }, value)
}

export const waitForText = async (page, sel, expected, timeout = 6000) => {
  const start = Date.now()
  for (; ;) {
    const t = await page.$eval(sel, (el) => el.textContent || "").catch(() => "")
    if (t.includes(expected)) return
    if (Date.now() - start > timeout) {
      throw new Error(`timed out waiting for "${expected}" in ${sel} (last: "${t}")`)
    }
    await new Promise((r) => setTimeout(r, 100))
  }
}

export const waitFor = async (predicate, timeout = 8000, interval = 150) => {
  const start = Date.now()
  for (; ;) {
    const value = await predicate()
    if (value) return value
    if (Date.now() - start > timeout) {
      throw new Error("timed out waiting for condition")
    }
    await new Promise((r) => setTimeout(r, interval))
  }
}

export const snap = async (page, name, {fullPage = true} = {}) => {
  await mkdir(VISUALS, {recursive: true})
  const file = path.join(VISUALS, `${name}.png`)
  await page.screenshot({path: file, fullPage})
  return file
}

export const freePort = () => new Promise((resolve, reject) => {
  const server = net.createServer()
  server.on("error", reject)
  server.listen(0, "127.0.0.1", () => {
    const port = server.address().port
    server.close(() => resolve(port))
  })
})

const startEchoServer = async () => {
  const sockets = new Set()
  const server = net.createServer((socket) => {
    sockets.add(socket)
    socket.on("close", () => sockets.delete(socket))
    socket.on("error", () => {
    })
    socket.pipe(socket)
  })
  await new Promise((resolve, reject) => {
    server.on("error", reject)
    server.listen(0, "127.0.0.1", resolve)
  })
  return {
    port: server.address().port,
    close: () => new Promise((resolve) => {
      for (const socket of sockets) {
        socket.destroy()
      }
      server.close(() => resolve())
    })
  }
}

export const sendTraffic = (port, bytes = 8192, count = 1) => {
  const payload = Buffer.alloc(bytes, 0x61)
  let remaining = count
  return new Promise((resolve, reject) => {
    const once = () => {
      const socket = net.createConnection({host: "127.0.0.1", port}, () => {
        socket.write(payload)
      })
      let received = 0
      socket.setTimeout(5000, () => {
        socket.destroy()
        reject(new Error("traffic timeout"))
      })
      socket.on("data", (chunk) => {
        received += chunk.length
        if (received >= payload.length) {
          socket.end()
          remaining = remaining - 1
          if (remaining > 0) {
            once()
          } else {
            resolve(count * payload.length)
          }
        }
      })
      socket.on("error", reject)
    }
    once()
  })
}

export const uniqueId = (prefix = "e2e") =>
  `${prefix}-${Date.now()}-${Math.floor(Math.random() * 10000)}`

export const seedConfig = async (cfg) => {
  const res = await request(apiUrl("/v1/config?configId=new"), {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify(cfg),
  })
  const body = await res.json().catch(() => null)
  if (!res.ok || (Array.isArray(body) && body.length > 0)) {
    throw new Error(`seed failed (${res.status}): ${JSON.stringify(body)}`)
  }
  return cfg
}

export const deleteConfig = async (id) => {
  await request(apiUrl(`/v1/config?configId=${encodeURIComponent(id)}`), {method: "DELETE"}).catch(() => {
  })
}

const activeConfig = async () => {
  const res = await request(apiUrl("/v1/config"))
  if (!res.ok) throw new Error(`active config failed (${res.status})`)
  return res.json()
}

const activateConfig = async (id) => {
  const res = await request(apiUrl(`/v1/config/select?configId=${encodeURIComponent(id)}`))
  if (!res.ok) throw new Error(`activate failed (${res.status})`)
  return res.json()
}

export const stopConfig = async () => {
  const res = await request(apiUrl("/v1/config/select"))
  if (!res.ok) throw new Error(`stop failed (${res.status})`)
  return res.json()
}

export const proxyConfigOf = (id, backendPort, proxyPort) => ({
  id,
  description: "Browser test configuration",
  servers: [{
    id: "http",
    addr: {host: "127.0.0.1", port: proxyPort},
    match: [{
      pool: {
        type: "roundRobin",
        hosts: [{addr: {host: "127.0.0.1", port: backendPort}}],
      },
    }],
  }],
})

/**
 * Seeds + activates a throwaway config backed by a Node TCP echo server, then
 * restores the previously active config and deletes the generated one.
 */
export const withProxy = async (fn) => {
  const echo = await startEchoServer()
  const proxyPort = await freePort()
  const id = uniqueId("e2e")
  let previous
  try {
    previous = await activeConfig().catch(() => undefined)
  } catch {
    previous = undefined
  }
  const cfg = proxyConfigOf(id, echo.port, proxyPort)
  try {
    await seedConfig(cfg)
    await activateConfig(id)
    return await fn({id, cfg, proxyPort, echo, previous})
  } finally {
    try {
      if (previous?.id) {
        await activateConfig(previous.id)
      } else {
        await stopConfig()
      }
    } catch {
    }
    await deleteConfig(id)
    await echo.close()
  }
}
