organization := "$organization$"

name := "$name$"

/*
 * Compatibility version.  Use this to declare what version with
 * which `master` remains in compatibility.  This is literally
 * backwards from how -SNAPSHOT versioning works, but it avoids
 * the need to pre-declare (before work is done) what kind of
 * compatibility properties the next version will have (i.e. major
 * or minor bump).
 *
 * As an example, the builds of a project might go something like
 * this:
 *
 * - 0.0-hash1
 * - 0.0-hash2
 * - 0.0-hash3
 * - 0.1
 * - 0.1-hash1
 * - 0.1-hash2
 * - 0.2
 * - 0.2-hash1
 * - 0.2-hash2
 * - 0.2-hash3
 * - 0.2-hash4
 * - 1.0
 *
 * The value of BaseVersion starts at 0.0, then increments to 0.1
 * when that release is tagged, and so on.  Again, this is all to
 * avoid pre-committing to a major/minor bump before the work is
 * done (see: Scala 2.8).
 */
val BaseVersion = "0.0"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/"))

/***********************************************************************\
                      Boilerplate below these lines
\***********************************************************************/

// parses Scala versions out of .travis.yml (doesn't support build matrices)
crossScalaVersions := {
  import org.yaml.snakeyaml.Yaml

  import scala.collection.JavaConverters._

  import java.io.{FileInputStream => FIS}
  import java.{util => ju}

  val yaml = new Yaml

  val fis = new FIS(baseDirectory.value / ".travis.yml")

  try {
    val list = Option(yaml.load(fis)) collect {
      case map: ju.Map[_, _] => Option(map.get("scala"))
    } flatten

    list collect {
      case versions: ju.List[_] => versions.asScala.toList map { _.toString }
    } getOrElse List("$scala_version$")
  } finally {
    fis.close()
  }
}

scalaVersion := crossScalaVersions.value.last

coursierUseSbtCredentials := true
coursierChecksums := Nil      // workaround for nexus sync bugs

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)

// Adapted from Rob Norris' post at https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions ++= Seq(
  "-language:_",
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code"
)

scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq(
      "-Ywarn-unused-import", // Not available in 2.10
      "-Ywarn-numeric-widen" // In 2.10 this produces a some strange spurious error
    )
    case _ => Seq.empty
  }
}

scalacOptions ++= {
  scalaVersion.value match {
    case "2.11.9" => Seq("-Ypartial-unification")
    case v if v startsWith "2.12" => Seq("-Ypartial-unification")
    case _ => Seq.empty
  }
}

scalacOptions in Test += "-Yrangepos"

scalacOptions in (Compile, console) ~= (_ filterNot (Set("-Xfatal-warnings", "-Ywarn-unused-import").contains))

scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

libraryDependencies ++= {
  scalaVersion.value match {
    case "2.11.8" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
    case "2.10.6" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
    case _ => Seq.empty
  }
}

enablePlugins(GitVersioning)

val ReleaseTag = """^v([\\d\\.]+)\$""".r

git.baseVersion := BaseVersion

git.gitTagToVersionNumber := {
  case ReleaseTag(version) => Some(version)
  case _ => None
}

git.formattedShaVersion := {
  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

  git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
    git.baseVersion.value + "-" + sha + suffix
  }
}
