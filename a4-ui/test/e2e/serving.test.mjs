import {after, before, test} from "node:test"
import assert from "node:assert/strict"
import {closeBrowser, goto, openPage, requireUi, UI_URL} from "../harness.mjs"

let page

before(async () => {
  if (await requireUi()) page = await openPage()
})

after(async () => closeBrowser())

test("serves the SPA shell and static assets", async () => {
  for (const p of ["/", "/config/list", "/traffic", "/index.js", "/index.css"]) {
    const res = await fetch(UI_URL + p)
    assert.equal(res.status, 200, `${p} status`)
    const body = await res.text()
    if (p.endsWith(".js") || p.endsWith(".css")) {
      assert.ok(body.length > 1000, `${p} should not be empty`)
    } else {
      assert.match(body, /index\.js/, `${p} should serve the SPA shell`)
      assert.match(body, /id="app"/)
    }
  }
})

test("deep links fall back to the SPA shell", async () => {
  if (!page) return
  await goto(page, "/config/does-not-exist/edit")
  await page.waitForSelector(".a4-header", {timeout: 5000})
  assert.match(await page.title(), /awe4lb/)
})

test("reports no console errors on the main routes", async () => {
  if (!page) return
  page.errors.length = 0
  for (const p of ["/", "/config/list", "/config/new/edit", "/traffic"]) {
    await goto(page, p)
    await page.waitForSelector(".a4-header")
  }
  assert.deepEqual(page.errors, [])
})
