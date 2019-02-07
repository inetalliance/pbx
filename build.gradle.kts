description = "CG2 PBX"
plugins {
    war
    id("com.bmuschko.cargo") version "2.5"
}
dependencies {
    compile(project(":obj"))
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compile("net.inetalliance.msg:aj:1.1-SNAPSHOT")
}
apply(from = "cargo.gradle")
