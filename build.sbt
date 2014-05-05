organization  := "com.threatstream"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
    "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases",
    "spray repo" at "http://repo.spray.io/",
    "SnowPlow Repo" at "http://maven.snplow.com/releases/",
    "Twitter Maven Repo" at "http://maven.twttr.com/",
    "Scala.sh releases" at "http://scala.sh/repositories/releases"
)

libraryDependencies ++= {
    val akkaV = "2.1.4"
    val sprayV = "1.1.0"
    Seq(
        "io.spray" % "spray-can" % sprayV,
        "io.spray" % "spray-routing" % sprayV,
        "io.spray" % "spray-testkit" % sprayV,
        "io.spray" %%  "spray-json" % "1.2.5",
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-testkit" % akkaV,
        "org.specs2" %% "specs2" % "2.2.3" % "test",
        "com.snowplowanalytics" %% "scala-maxmind-geoip" % "0.0.5",
        "com.amazonaws" % "aws-java-sdk"  % "1.6.12"
    )
}

//seq(Revolver.settings: _*)
Revolver.settings

