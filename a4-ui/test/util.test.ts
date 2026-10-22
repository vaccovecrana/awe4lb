import {test} from "node:test"
import assert from "node:assert/strict"
import {
  formatBytes,
  formatDuration,
  formatRate,
  numOrUndefined,
  splitArgs,
  splitList,
  statusClass
} from "../@a4ui/util.ts"

test("splits command arguments with quotes", () => {
  assert.deepEqual(splitArgs("-z -v $host $port"), ["-z", "-v", "$host", "$port"])
  assert.deepEqual(splitArgs(`-c "hello world" 'a b'`), ["-c", "hello world", "a b"])
  assert.deepEqual(splitArgs("  "), [])
})

test("splits comma and newline separated lists", () => {
  assert.deepEqual(splitList("a, b\nc"), ["a", "b", "c"])
  assert.deepEqual(splitList(" , "), [])
})

test("formats byte and rate values", () => {
  assert.equal(formatBytes(0), "0 B")
  assert.equal(formatBytes(1024), "1.0 KiB")
  assert.equal(formatBytes(10 * 1024 * 1024), "10 MiB")
  assert.equal(formatRate(1536), "1.5 KiB/s")
  assert.equal(formatRate(undefined), "0 B/s")
})

test("formats durations compactly", () => {
  assert.equal(formatDuration(undefined), "?")
  assert.equal(formatDuration(500), "500 ms")
  assert.equal(formatDuration(5000), "5s")
  assert.equal(formatDuration(2500), "2.5s")
  assert.equal(formatDuration(9900), "9.9s")
})

test("parses optional numbers", () => {
  assert.equal(numOrUndefined(""), undefined)
  assert.equal(numOrUndefined("   "), undefined)
  assert.equal(numOrUndefined("42"), 42)
  assert.equal(numOrUndefined("nope"), undefined)
})

test("maps backend states to status classes", () => {
  assert.match(statusClass("Up"), /a4-status-up/)
  assert.match(statusClass("Down"), /a4-status-down/)
  assert.match(statusClass(undefined), /a4-status-unknown/)
})
