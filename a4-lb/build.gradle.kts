plugins {
  application
  id("org.graalvm.buildtools.native") version "0.10.2"
}

dependencies {
  implementation(project(":a4-ui"))
}

application {
  mainClass.set("io.vacco.a4lb.A4LbMain")
}

graalvmNative {
  binaries {
    named("main") {
      configurationFileDirectories.from(file("src/main/resources"))
      buildArgs.add("--enable-url-protocols=http")
      buildArgs.add("-march=compatibility")
    }
  }
}

val copyLic = tasks.register<Copy>("copyLic") {
  from("../NOTICE", "../LICENSE")
  into("build/resources/main")
}

tasks.processResources {
  dependsOn(copyLic)
}
