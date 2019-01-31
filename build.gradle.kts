description = "CG2 PBX"
apply(plugin = "war")

dependencies {
    compile(project(":obj"))
    compileOnly("javax.servlet:javax.servlet-api:3.1.0")
    compile("net.inetalliance.msg:aj:1.1-SNAPSHOT")
}
