import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
 
object ProjectBuild extends Build {
  val Organization = "wysa"
  val Version      = "0.1.0"
  val ScalaVersion = "2.10.0-RC1"
  val sep = System.getProperty("file.separator")
  lazy val project = Project(
    id = "scalah",
    base = file("."),
    settings = defaultSettings ++ 
    assemblySettings ++
    Seq(
      // mainClass in assembly := Some("com.innoxyz.playground.playasync.HttpServer")
      // javaOptions in run += "-Djava.library.path=F:\\c++_project\\WindowsAIO\\x64\\Release",
      // fork in run := true
      unmanagedClasspath in Compile += Attributed.blank(file(System.getenv("JAVA_HOME")+sep+"lib"+sep+"tools.jar"))
    )
  )
 
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version      := Version,
    scalaVersion := ScalaVersion,
    crossPaths   := false
  )
  
  lazy val defaultSettings = buildSettings ++ Seq(
    // resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
 
    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")


 
  )
}

 
object Dependency {
}