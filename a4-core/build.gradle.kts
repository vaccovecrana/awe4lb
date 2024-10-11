plugins { id("io.vacco.oss.gitflow") }

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.16.0.1.2")
  api("am.ik.yavi:yavi:0.14.1")
  api("org.buildobjects:jproc:2.8.2")
  api("com.google.code.gson:gson:2.11.0")
}
