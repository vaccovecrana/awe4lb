import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("io.vacco.oss.gitflow") version "0.9.8"
  id("io.vacco.ronove") version "1.2.3"
  id("com.github.node-gradle.node") version "7.0.1"
}

val api by configurations

dependencies {
  api(project(":a4-core"))
  api("io.vacco.ronove:rv-kit-murmux:1.2.3_2.2.2")
}

configure<io.vacco.ronove.plugin.RvPluginExtension> {
  controllerClasses = arrayOf("io.vacco.a4lb.web.A4ApiHdl")
  outFile.set(file("./@a4ui/rpc.ts"))
}

node {
  download.set(true)
  version.set("18.16.0")
}

val buildTaskUsingNpm = tasks.register<NpmTask>("buildNpm") {
  dependsOn(tasks.npmInstall)
  npmCommand.set(listOf("run", "build"))
  inputs.dir("./@a4ui")
  outputs.dir("./build/ui")
}

val copyJs = tasks.register<Copy>("copyJs") {
  dependsOn(buildTaskUsingNpm)
  from("./build/ui")
  from("./res/favicon.svg")
  into("./build/resources/main/ui")
}

val copyTs = tasks.register<Copy>("copyTs") {
  dependsOn(buildTaskUsingNpm)
  from("./@a4ui")
  into("./build/resources/main/ui/@a4ui")
}

val copyRes = tasks.register<Copy>("copyRes") {
  dependsOn(buildTaskUsingNpm)
  from("./node_modules/simple-line-icons/fonts")
  into("./build/resources/main/ui/fonts")
}

tasks.processResources {
  dependsOn(copyJs)
  dependsOn(copyTs)
  dependsOn(copyRes)
}
