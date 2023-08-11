configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
  sharedLibrary(true, false)
}

val api by configurations

dependencies {
  api("org.slf4j:slf4j-api:2.0.6")
  api("com.github.chrisvest:stormpot:3.1")
  testImplementation("io.vacco.shax:shax:2.0.6.0.1.0")
}
