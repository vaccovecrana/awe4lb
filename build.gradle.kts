plugins {
  id("io.vacco.oss.gitflow") version "0.9.8"
  id("io.vacco.ronove") version "1.2.2"
  application
}

group = "io.vacco.awe4lb"
version = "0.1.0"

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addClasspathHell()
  addJ8Spec()
  sharedLibrary(true, false)
}

configure<io.vacco.cphell.ChPluginExtension> {
  resourceExclusions.add("module-info.class")
}

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.6.0.1.0")
  api("am.ik.yavi:yavi:0.13.1")
  api("org.buildobjects:jproc:2.8.2")
  api("com.google.code.gson:gson:2.10.1")
  api("io.vacco.ronove:rv-kit-murmux:2.2.2")
}

configure<io.vacco.ronove.plugin.RvPluginExtension> {
  controllerClasses = arrayOf("io.vacco.a4lb.api.A4Controller")
  outFile.set(file("../a4lb-ui/@a4-ui/rpc.ts"))
}

application {
  mainClass.set("io.vacco.a4lb.A4Lb")
}

