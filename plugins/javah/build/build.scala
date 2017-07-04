package javah_build

import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies = super.dependencies ++ Resolver(mavenCentral).bind(
    MavenDependency("org.ow2.asm", "asm", "5.0.4")
  )
}
