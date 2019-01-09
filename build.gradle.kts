description = "CG2 PBX"
apply(plugin= "war")

dependencies {
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")
  compile(project(":crm:obj"))
  compile(project(":msg:aj"))
}
