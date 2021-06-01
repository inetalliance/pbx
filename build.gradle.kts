description = "CG2 PBX"
plugins {
  war
  id("com.bmuschko.cargo") version "2.5"
}
dependencies {
  implementation(project(":obj"))
  implementation("net.inetalliance.angular:base:1.1-SNAPSHOT")
	implementation("joda-time:joda-time:2.2")
  implementation("net.inetalliance:util:1.1-SNAPSHOT")
  implementation("net.inetalliance:daemonic:1.1-SNAPSHOT")
  implementation("net.inetalliance:log:1.1-SNAPSHOT")
  implementation("net.inetalliance:cli:1.1-SNAPSHOT")
  implementation("net.inetalliance:sql:1.1-SNAPSHOT")
  implementation("net.inetalliance:validation:1.1-SNAPSHOT")
  implementation("net.inetalliance:potion:6.1-SNAPSHOT")
  implementation("net.inetalliance:funky:1.1-SNAPSHOT")
  implementation("net.inetalliance:types:1.1-SNAPSHOT")
	implementation("org.asteriskjava:asterisk-java:1.0.0-final")
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")
  implementation("net.inetalliance.msg:aj:1.1-SNAPSHOT")
  implementation("net.inetalliance.msg:bjx:6.1-SNAPSHOT")
  runtimeOnly("org.postgresql:postgresql:42.2.5")
}
apply(from = "cargo.gradle")
tasks {
  register("deploy") {
    dependsOn("cargoDeployRemote")
  }
}
