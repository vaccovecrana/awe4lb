configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.6.0.1.0")
  api("com.github.chrisvest:stormpot:3.1")
  api("com.github.marianobarrios:tls-channel:0.8.0")
}
