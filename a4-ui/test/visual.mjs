// Captures desktop + mobile screenshots for visual inspection.
// Requires a running awe4lb instance:
//   A4_UI_URL=http://127.0.0.1:7070 npm run visual
import {
  closeBrowser, goto, openPage, requireUi, sendTraffic, snap, stopConfig, withProxy
} from "./harness.mjs"

const VIEWPORTS = [
  {name: "desktop", width: 1440, height: 1000},
  {name: "mobile", width: 390, height: 844},
]

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const capture = async (page, viewport, name, urlPath) => {
  await page.setViewport({width: viewport.width, height: viewport.height})
  await goto(page, urlPath)
  await page.waitForSelector(".a4-header", {timeout: 8000})
  await wait(400)
  return snap(page, `${name}-${viewport.name}`)
}

const main = async () => {
  if (!(await requireUi())) {
    process.exit(0)
  }
  const page = await openPage()
  try {
    await withProxy(async ({id, proxyPort}) => {
      await sendTraffic(proxyPort, 8192, 4)
      await wait(2200)
      const routes = [
        ["status", "/"],
        ["configs", "/config/list"],
        ["editor", `/config/${id}/edit`],
        ["traffic", "/traffic"],
      ]
      for (const viewport of VIEWPORTS) {
        for (const [name, urlPath] of routes) {
          const file = await capture(page, viewport, name, urlPath)
          console.log(file)
        }
      }

      await stopConfig()
      for (const viewport of VIEWPORTS) {
        const file = await capture(page, viewport, "status-empty", "/")
        console.log(file)
      }
    })
  } finally {
    await closeBrowser()
  }
}

main().then(() => {
  process.exit(0)
}).catch((e) => {
  console.error(e)
  process.exit(1)
})
