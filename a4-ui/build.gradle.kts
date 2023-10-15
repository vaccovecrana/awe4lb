import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("io.vacco.oss.gitflow") version "0.9.8"
  id("io.vacco.ronove") version "1.2.2"
  id("com.github.node-gradle.node") version "7.0.1"
  application
}

val api by configurations

dependencies {
  api(project(":a4-core"))
  api("io.vacco.ronove:rv-kit-murmux:2.2.2")
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

val copyRes = tasks.register<Copy>("copyJs") {
  dependsOn(buildTaskUsingNpm)
  from("./build/ui")
  from("./res/favicon.svg")
  into("./build/resources/main/ui")
}

tasks.processResources {
  dependsOn(copyRes)
}

application {
  mainClass.set("io.vacco.a4lb.A4LbMain")
}
