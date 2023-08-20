configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.6.0.1.0")
  api("com.github.marianobarrios:tls-channel:0.8.0")
  api("am.ik.yavi:yavi:0.13.1")
  api("org.buildobjects:jproc:2.8.2")
  api("org.slf4j:jul-to-slf4j:2.0.6")
}
