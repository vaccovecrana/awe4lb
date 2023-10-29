dependencies {
  testImplementation(project(":a4-core"))
  testImplementation(project(":a4-ui"))
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