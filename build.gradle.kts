description = "CG2 PBX"
apply(plugin= "war")

dependencies {
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")
  if(findProject(":potion") == null) {
    compile(project(":obj"))
    compile("net.inetalliance.msg:aj:1.1-SNAPSHOT")
  } else {
    compile(project(":crm:obj"))
    compile(project(":msg:aj"))
  }
}
