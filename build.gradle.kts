description = "CG2 PBX"
plugins {
    war
		id("java-library")
}
dependencies {
    api(project(":sonar:obj"))
    api(project(":libs:types"))
    api(project(":sonar:angular"))
    api(project(":msg:aj"))

    implementation(libs.asterisk)
    compileOnly(libs.servlet)
    runtimeOnly(libs.postgresql)
}
