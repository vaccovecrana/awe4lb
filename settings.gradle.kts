pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    repositories {
      maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      }
    }
  }
}

include("a4-core", "a4-ui", "a4-lb", "a4-test")

project(":a4-lb").name = "a4lb-${System.getProperty("os.name").lowercase().replace(" ", "")}-${System.getProperty("os.arch")}"
