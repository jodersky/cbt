package cbt
import java.net.URL
import scala.language.postfixOps
import scala.sys.process._

trait GitVersion extends BaseBuild {
  def version: String =
    (s"git -C $projectDirectory describe --always --dirty=-SNAPSHOT --match v[0-9].*" !!).tail.trim
}
