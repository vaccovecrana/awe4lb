import {after, before, test} from "node:test"
import assert from "node:assert/strict"
import {closeBrowser, goto, openPage, requireUi, sendTraffic, text, waitForText, withProxy} from "../harness.mjs"

let page

before(async () => {
  if (await requireUi()) page = await openPage()
})

after(async () => closeBrowser())

test("shows live charts and backend counters for proxied traffic", async () => {
  if (!page) return
  await withProxy(async ({proxyPort, echo}) => {
    await sendTraffic(proxyPort, 4096, 3)
    await new Promise((r) => setTimeout(r, 2200))

    await goto(page, "/traffic")
    await waitForText(page, "body", "127.0.0.1", 10000)
    assert.ok(await page.$(".a4-chart canvas"), "chart canvas should render")
    const rows = await page.$$(".a4-table tbody tr")
    assert.ok(rows.length >= 1, "backend table should have a row")
    assert.ok((await text(page, ".a4-stat-grid")).toLowerCase().includes("active connections"))

    const table = await text(page, ".a4-table")
    assert.match(table, new RegExp(String(echo.port)))
  })
})

test("pauses and resumes polling", async () => {
  if (!page) return
  await goto(page, "/traffic")
  await page.waitForSelector('[data-testid="traffic-pause"]')
  assert.equal(await text(page, '[data-testid="traffic-pause"]'), "Pause")
  await page.click('[data-testid="traffic-pause"]')
  await waitForText(page, '[data-testid="traffic-pause"]', "Resume")
  await page.click('[data-testid="traffic-pause"]')
  await waitForText(page, '[data-testid="traffic-pause"]', "Pause")
})
