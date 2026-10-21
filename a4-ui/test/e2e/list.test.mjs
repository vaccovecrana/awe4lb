import {after, before, test} from "node:test"
import assert from "node:assert/strict"
import {
  closeBrowser, deleteConfig, freePort, goto, openPage, proxyConfigOf, requireUi, seedConfig,
  text, uniqueId, waitFor, waitForText, withProxy
} from "../harness.mjs"

let page

before(async () => {
  if (await requireUi()) page = await openPage()
})

after(async () => closeBrowser())

test("lists a seeded configuration and toggles its active state", async () => {
  if (!page) return
  await withProxy(async ({id}) => {
    await goto(page, "/config/list")
    const card = `[data-config-id="${id}"]`
    await page.waitForSelector(card, {timeout: 5000})
    const body = await text(page, card)
    assert.match(body, /Browser test configuration/)
    assert.match(body, /1 server/)
    assert.match(body, /Active/)

    await page.click(`${card} [data-action="toggle-active"]`)
    await waitForText(page, card, "Stopped")
    await page.click(`${card} [data-action="toggle-active"]`)
    await waitForText(page, card, "Active")
  })
})

test("shows the new-configuration action", async () => {
  if (!page) return
  await goto(page, "/config/list")
  assert.ok(await page.$('[data-testid="new-config"]'))
})

test("deletes a configuration after confirmation", async () => {
  if (!page) return
  const id = uniqueId("e2e-delete")
  await seedConfig(proxyConfigOf(id, 1, await freePort()))
  await goto(page, "/config/list")
  const card = `[data-config-id="${id}"]`
  await page.waitForSelector(card, {timeout: 5000})
  await page.click(`${card} [data-action="delete"]`)
  await waitFor(async () => !(await page.$(card)), 6000)
  await deleteConfig(id)
})
