organization  := "com.threatstream"

name          := "GeoSpray"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
    "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases",
    "spray repo" at "http://repo.spray.io/",
    "SnowPlow Repo" at "http://maven.snplow.com/releases/",
    "Twitter Maven Repo" at "http://maven.twttr.com/",
    "Scala.sh Maven2 releases" at "http://scala.sh/repositories/releases"
)

libraryDependencies ++= {
    val akkaV = "2.1.4"
    val sprayV = "1.2.1"
    Seq(
        "io.spray" % "spray-can" % sprayV,
        "io.spray" % "spray-routing" % sprayV,
        "io.spray" % "spray-testkit" % sprayV,
        "io.spray" %%  "spray-json" % "1.2.5",
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-testkit" % akkaV,
        "org.specs2" %% "specs2" % "2.2.3" % "test",
        "com.snowplowanalytics" %% "scala-maxmind-iplookups" % "0.1.0",
        "com.amazonaws" % "aws-java-sdk" % "1.8.6"
    )
}

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

sublimeTransitive := true

//seq(Revolver.settings: _*)
Revolver.settings


val downloader = taskKey[Unit]("downloader")

downloader := {
    //val process = java.lang.Runtime.getRuntime().exec("ls -al /");
    //val reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))
    //while (reader.ready()) {
    //    println(reader.readLine())
    //}
    val prefix = java.lang.System.getenv("HOME") + "/"
    println("creating aws auth")
    val auth = new com.amazonaws.auth.BasicAWSCredentials(java.lang.System.getenv("AWS_ACCESS_KEY"), java.lang.System.getenv("AWS_SECRET_KEY"))
    println("creating aws client")
    val client = new com.amazonaws.services.s3.AmazonS3Client(auth)
    Seq("GeoLiteCity.dat", "GeoIPNetspeed.dat", "GeoIPOrg.dat", "GeoIPASNum.dat") foreach { fileName =>
        val dbFile = new java.io.File(prefix + fileName)
        if (!dbFile.exists) {
            println("downloading geoip db from s3: " + fileName)
            client.getObject(new com.amazonaws.services.s3.model.GetObjectRequest(System.getenv("AWS_S3_BUCKET_NAME"), fileName), dbFile)
        } else {
            println("arleady exists: " + prefix + fileName)
        }
    }
    println("done")
}

downloader <<= downloader triggeredBy (compile in Compile)

