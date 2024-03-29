repositories {
  mavenLocal()
}

dependencies {
  testImplementation(project(":a4-core"))
  testImplementation(project(":a4-ui"))
  testImplementation("com.github.mizosoft.methanol:methanol:1.7.0")
}

configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
}

tasks.withType<JacocoReport> {
  sourceSets(
      project(":a4-core").sourceSets.main.get(),
      project(":a4-ui").sourceSets.main.get(),
  )
}