import {after, before, test} from "node:test"
import assert from "node:assert/strict"
import {closeBrowser, goto, openPage, requireUi, text} from "../harness.mjs"

let page

before(async () => {
  if (await requireUi()) page = await openPage()
})

after(async () => closeBrowser())

const activeNav = (page) => text(page, ".a4-nav a.active")

test("highlights the active navigation item on each route", async () => {
  if (!page) return
  await goto(page, "/")
  assert.equal(await activeNav(page), "Status")
  await goto(page, "/config/list")
  assert.equal(await activeNav(page), "Configs")
  await goto(page, "/traffic")
  assert.equal(await activeNav(page), "Traffic")
  await goto(page, "/config/new/edit")
  assert.equal(await activeNav(page), "Configs")
})

test("keeps the header visible and stable across navigation", async () => {
  if (!page) return
  await goto(page, "/")
  const brand = await text(page, ".a4-brand")
  assert.match(brand, /awe4lb/)
  await page.click(".a4-nav a[href='/traffic']")
  await page.waitForSelector(".a4-chart-panel", {timeout: 5000})
  assert.match(await text(page, ".a4-brand"), /awe4lb/)
})
