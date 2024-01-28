plugins {
  application
}

dependencies {
  implementation(project(":a4-ui"))
}

application {
  mainClass.set("io.vacco.a4lb.A4LbMain")
}
