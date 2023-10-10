plugins {
  application
  id("io.vacco.ronove") version "1.2.1"
}

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.6.0.1.0")
  api("am.ik.yavi:yavi:0.13.1")
  api("org.buildobjects:jproc:2.8.2")
  api("com.google.code.gson:gson:2.10.1")
  api("io.vacco.ronove:rv-kit-murmux:1.2.1")
}

configure<io.vacco.ronove.plugin.RvPluginExtension> {
  controllerClasses = arrayOf("io.vacco.a4lb.api.A4Controller")
  outFile.set(file("../a4lb-ui/@a4-ui/rpc.ts"))
}

application {
  mainClass.set("io.vacco.a4lb.A4Lb")
}
