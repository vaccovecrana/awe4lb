plugins { id("io.vacco.oss.gitflow") version "0.9.8" }

val api by configurations

dependencies {
  api("io.vacco.shax:shax:2.0.6.0.1.0")
  api("am.ik.yavi:yavi:0.13.1")
  api("org.buildobjects:jproc:2.8.2")
  api("com.google.code.gson:gson:2.10.1")
}
