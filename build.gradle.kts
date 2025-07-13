plugins {
  id("io.vacco.oss.gitflow") version "1.8.0" apply(false)
}

subprojects {
  apply(plugin = "io.vacco.oss.gitflow")
  group = "io.vacco.awe4lb"
  version = "0.7.0"

  configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
    addClasspathHell()
  }

  configure<io.vacco.cphell.ChPluginExtension> {
    resourceExclusions.add("module-info.class")
  }
}
