plugins {
  id("io.vacco.oss.gitflow") version "1.10.0" apply(false)
}

subprojects {
  apply(plugin = "io.vacco.oss.gitflow")
  group = "io.vacco.awe4lb"
  version = "0.7.1"

  configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
    orgConfigUrl("https://raw.githubusercontent.com/vaccovecrana/org-config/refs/heads/main/vacco-oss-java-21.json")
    addClasspathHell()
  }

  configure<io.vacco.cphell.ChPluginExtension> {
    resourceExclusions.add("module-info.class")
  }
}
