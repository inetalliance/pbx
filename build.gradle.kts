description = "CG2 PBX"
plugins {
    war
}
dependencies {
    api(project(":api:obj"))
    api(project(":libs:types"))
    api(project(":api:angular"))
    api(project(":msg:aj"))

    implementation(libs.asterisk)
    compileOnly(libs.servlet)
    runtimeOnly(libs.postgresql)
}
