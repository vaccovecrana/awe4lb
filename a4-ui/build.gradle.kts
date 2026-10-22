import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("io.vacco.oss.gitflow")
  id("io.vacco.ronove") version "4.0.1"
  id("com.github.node-gradle.node") version "7.0.1"
}

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  sharedLibrary(true, false)
}

val api = configurations.getByName("api")

dependencies {
  api(project(":a4-core"))
  api("io.vacco.ronove:rv-kit-murmux:4.0.1")
}

configure<io.vacco.ronove.plugin.RvPluginExtension> {
  optionalFields = true
  controllerClasses = arrayOf("io.vacco.a4lb.web.A4ApiHdl")
  outFile.set(file("./@a4ui/rpc.ts"))
  reflectConfigFile.set(file("../a4-lb/src/main/resources/reflect-config.json"))
}

node {
  download.set(true)
  version.set("22.17.0")
}

val buildTaskUsingNpm = tasks.register<NpmTask>("buildNpm") {
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "build"))
  inputs.dir("./@a4ui")
  inputs.dir("./res")
  inputs.file("./rspack.config.js")
  inputs.file("./tsconfig.json")
  inputs.file("./package.json")
  outputs.dir("./build/ui")
}

val testTaskUsingNpm = tasks.register<NpmTask>("testUi") {
  group = "verification"
  description = "Runs the a4-ui unit test suite"
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "test:unit"))
  inputs.dir("./@a4ui")
  inputs.dir("./test")
  outputs.upToDateWhen { false }
}

val copyBundle = tasks.register<Copy>("copyBundle") {
  dependsOn(buildTaskUsingNpm)
  from("./build/ui")
  from("./res/favicon.svg")
  from("./res/index.html")
  into("./build/resources/main/ui")
}

tasks.processResources {
  dependsOn(copyBundle)
}

tasks.check {
  dependsOn(testTaskUsingNpm)
}

/**
 * Browser E2E suite against a running awe4lb instance. Not wired into `build`:
 *
 *   A4_UI_URL=http://127.0.0.1:7070 gradle :a4-ui:e2eTest
 */
tasks.register<NpmTask>("e2eTest") {
  description = "Runs the Puppeteer browser suite against a running UI"
  group = "verification"
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "test:e2e"))
  environment.put("A4_UI_URL", System.getenv("A4_UI_URL") ?: "http://127.0.0.1:7070")
  outputs.upToDateWhen { false }
}

/**
 * Captures desktop + mobile screenshots for visual inspection. Requires a
 * running UI:
 *
 *   A4_UI_URL=http://127.0.0.1:7070 gradle :a4-ui:visual
 *
 * Artifacts land in a4-ui/build/test-artifacts/visual/.
 */
tasks.register<NpmTask>("visual") {
  description = "Captures UI screenshots against a running instance"
  group = "verification"
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "visual"))
  environment.put("A4_UI_URL", System.getenv("A4_UI_URL") ?: "http://127.0.0.1:7070")
  outputs.upToDateWhen { false }
}
