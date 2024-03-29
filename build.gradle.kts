plugins { id("io.vacco.oss.gitflow") version "0.9.8" apply(false) }

subprojects {
  apply(plugin = "io.vacco.oss.gitflow")
  group = "io.vacco.awe4lb"
  version = "0.1.0"

  configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
    addClasspathHell()
    sharedLibrary(true, false)
  }

  configure<io.vacco.cphell.ChPluginExtension> {
    resourceExclusions.add("module-info.class")
  }
}
