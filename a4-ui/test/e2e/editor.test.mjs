import {after, before, test} from "node:test"
import assert from "node:assert/strict"
import {
  closeBrowser, deleteConfig, goto, openPage, requireUi, setInput, uniqueId, waitForText
} from "../harness.mjs"

let page

before(async () => {
  if (await requireUi()) page = await openPage()
})

after(async () => closeBrowser())

const yamlValue = (page) => page.$eval('[data-testid="yaml-editor"]', (el) => el.value)
const inputValue = (page, sel) => page.$eval(sel, (el) => el.value)

const sampleYaml = (id) => `id: ${id}
description: From YAML
servers:
  - id: http
    addr: {host: 127.0.0.1, port: 18080}
    match:
      - pool:
          type: roundRobin
          hosts:
            - addr: {host: 127.0.0.1, port: 9000}
`

test("keeps the form and YAML document in sync in both directions", async () => {
  if (!page) return
  await goto(page, "/config/new/edit")
  await page.waitForSelector('[data-testid="yaml-editor"]')

  await setInput(page, '[data-testid="config-description"]', "Form to YAML")
  assert.match(await yamlValue(page), /Form to YAML/)

  const id = uniqueId("e2e-yaml")
  await setInput(page, '[data-testid="yaml-editor"]', sampleYaml(id))
  await waitForText(page, "body", "Unsaved changes")
  assert.equal(await inputValue(page, '[data-testid="config-id"]'), id)
  assert.equal(await inputValue(page, '[data-testid="config-description"]'), "From YAML")
  assert.equal(await inputValue(page, '[data-testid="server-port"]'), "18080")
  assert.equal(await inputValue(page, '[data-testid="host-host"]'), "127.0.0.1")
})

test("reports malformed YAML with a line position", async () => {
  if (!page) return
  await goto(page, "/config/new/edit")
  await page.waitForSelector('[data-testid="yaml-editor"]')
  await setInput(page, '[data-testid="yaml-editor"]', "id: [broken\nservers: []\n")
  await page.waitForSelector(".a4-error", {timeout: 5000})
  const error = await page.$eval(".a4-error", (el) => el.textContent)
  assert.match(error, /YAML/)
  assert.match(error, /line/i)
})

test("blocks saving an invalid configuration id", async () => {
  if (!page) return
  await goto(page, "/config/new/edit")
  await page.waitForSelector('[data-testid="config-id"]')
  await setInput(page, '[data-testid="config-id"]', "Bad Id!")
  await page.click('[data-testid="save-config"]')
  await page.waitForSelector(".a4-error", {timeout: 5000})
  assert.match(await page.$eval(".a4-error", (el) => el.textContent), /Invalid configuration id/i)
})

test("saves a valid configuration and navigates to its editor", async () => {
  if (!page) return
  const id = uniqueId("e2e-save")
  try {
    await goto(page, "/config/new/edit")
    await page.waitForSelector('[data-testid="config-id"]')
    await setInput(page, '[data-testid="config-id"]', id)
    await setInput(page, '[data-testid="config-description"]', "Saved by E2E")
    await page.click('[data-testid="save-config"]')
    await page.waitForFunction(
      (cid) => window.location.pathname === `/config/${cid}/edit`,
      {timeout: 8000},
      id
    )
    await page.waitForSelector('[data-testid="yaml-editor"]')
    assert.equal(await inputValue(page, '[data-testid="config-id"]'), id)
  } finally {
    await deleteConfig(id)
  }
})
