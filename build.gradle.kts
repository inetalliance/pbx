description = "CG2 PBX"
plugins {
    war
    id("com.bmuschko.cargo") version "2.5"
}
dependencies {
    compile(project(":obj"))
    compile("net.inetalliance.angular:base:1.1-SNAPSHOT")
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compile("net.inetalliance.msg:aj:1.1-SNAPSHOT")
    runtime("org.postgresql:postgresql:42.2.5")
}
apply(from = "cargo.gradle")
tasks {
    register("deploy") {
        dependsOn("cargoDeployRemote")
    }
}
