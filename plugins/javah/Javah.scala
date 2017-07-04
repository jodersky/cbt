package cbt

import java.io.{ File, FileInputStream, Closeable }
import java.nio.file._
import scala.collection.JavaConverters._
import scala.collection.mutable.{ HashSet }
import scala.sys.process._

import org.objectweb.asm.{ ClassReader, ClassVisitor, MethodVisitor, Opcodes }

trait Javah extends BaseBuild {

  def javah = Javah.apply(lib, println _).config(
    (target / "include").toPath,
    exportedClasspath.files ++ dependencyClasspath.files,
    exportedClasspath.files
      .flatMap(file => Files.walk(file.toPath).iterator().asScala.toSeq)
      .toSet
      .filter(Files.isRegularFile(_))
      .flatMap(Javah.findNativeClasses)
  )

}

object Javah {

  case class apply(lib: Lib, log: (String) => Unit) {
    case class config(target: Path, classpath: Seq[File], nativeClasses: Set[String]) {
      def apply: Set[String] = {
        val cp = classpath.mkString(sys.props("path.separator"))
        if (!Files.exists(target)) {
          log("creating file")
        }
        if (!nativeClasses.isEmpty) {
          log(s"headers will be generated in $target")
        }
        for (clazz <- nativeClasses) yield {
          log(s"generating header for $clazz")
          val parts = Seq(
            "javah",
            "-d", target.toString,
            "-classpath", cp,
            clazz
          )
          val cmd = parts.mkString(" ")
          Process(cmd).!!
        }
      }
    }
  }


  private class NativeFinder extends ClassVisitor(Opcodes.ASM5) {

    // classes found to contain at least one @native def
    val _nativeClasses = new HashSet[String]
    def nativeClasses = _nativeClasses.toSet

    private var fullyQualifiedName: String = ""

    override def visit(version: Int, access: Int, name: String, signature: String,
      superName: String, interfaces: Array[String]): Unit = {
      fullyQualifiedName = name.replaceAll("/", ".")
    }

    override def visitMethod(access: Int, name: String, desc: String,
      signature: String, exceptions: Array[String]): MethodVisitor = {

      val isNative = (access & Opcodes.ACC_NATIVE) != 0

      if (isNative) {
        _nativeClasses += fullyQualifiedName
      }

      null //return null, do not visit method further
    }

  }

  /** Finds classes containing native implementations.
    * @param classFile java class file from which classes are read
    * @return all fully qualified names of classes that contain at least one member annotated
    * with @native
    */
  def findNativeClasses(classFile: Path): Set[String]  = {
    val in = Files.newInputStream(classFile)
    try {
      val reader = new ClassReader(in)
      val finder = new NativeFinder
      reader.accept(finder, 0)
      finder.nativeClasses
    } finally {
      in.close()
    }
  }

}
