organization := "$organization$"

name := "$name$"

val NextVersion = "0.1"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/"))

crossScalaVersions := List("$scala_version$")
scalaVersion := crossScalaVersions.value.last

coursierUseSbtCredentials := true
coursierChecksums := Nil      // workaround for nexus sync bugs

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)

scalacOptions ++= Seq("-language:_")

scalacOptions ++= {
  scalaVersion.value match {
    case "2.11.9" => Seq("-Ypartial-unification")
    case v if v startsWith "2.12" => Seq("-Ypartial-unification")
    case _ => Seq.empty
  }
}

libraryDependencies ++= {
  scalaVersion.value match {
    case "2.11.8" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
    case "2.10.6" => Seq(compilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full))
    case _ => Seq.empty
  }
}

scalacOptions in Test += "-Yrangepos"

enablePlugins(GitVersioning)

val ReleaseTag = """^v([\\d\\.]+)\$""".r

git.baseVersion := NextVersion

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
