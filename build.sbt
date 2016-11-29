name := "new/s/leak"

version := "0.0.1"

scalaVersion := "2.11.7"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc41", // PostgreSQL Licence
  "org.scalikejdbc" %% "scalikejdbc" % "2.5.0", // ApacheV2
  "org.scalikejdbc" %% "scalikejdbc-config" % "2.5.0", // ApacheV2
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.1", // ApacheV2
  "org.elasticsearch" % "elasticsearch" % "2.3.5", // ApacheV2
  "commons-io" % "commons-io" % "2.4"
)

routesImport += "util.Binders._"

routesGenerator := InjectedRoutesGenerator

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

// Run `make build` before `sbt dist`
lazy val webpackBuild = taskKey[Unit]("Build production js bundle.")

webpackBuild := {
  "make build" !
}

(packageBin in Universal) <<= (packageBin in Universal) dependsOn webpackBuild

// Disable documentation to speed up compilation
// sources in (Compile, doc) := Seq.empty

scalacOptions in (Compile, doc) := List(
	"-skip-packages",  Seq("router", "scalikejdbc").mkString(":"),
	"-doc-title", "Newsleak ScalaDoc"
)

publishArtifact in (Compile, packageDoc) := false

// Configure the steps of the asset pipeline (used in stage and dist tasks)
// TODO: ***remove rjs for non-production deployment***
// rjs = RequireJS, uglifies, shrinks to one file, replaces WebJars with CDN
// digest = Adds hash to filename
// gzip = Zips all assets, Asset controller serves them automatically when client accepts them
pipelineStages := Seq(/*rjs,*/ digest, gzip)

// The r.js optimizer won't find jsRoutes so we must tell it to ignore it
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))
