package com.threatstream

import akka.actor.Actor
import akka.event.Logging
import spray.routing._
import spray.http._
import MediaTypes._

import com.snowplowanalytics.maxmind.geoip.{IpGeo, IpLocation}

import spray.json._
//import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

    // the HttpService trait defines only one abstract member, which
    // connects the services environment to the enclosing actor or test
    def actorRefFactory = context

    // this actor only runs our route, but you could add
    // other things here, like request stream processing
    // or timeout handling
    def receive = runRoute(myRoute)
}

object ExtendedJsonProtocol extends DefaultJsonProtocol {
    implicit val ipLocationFormat = jsonFormat10(IpLocation.apply)
}

import ExtendedJsonProtocol._


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

    val ipGeo = {
        import java.io.File
        import com.amazonaws.services.s3.AmazonS3Client
        import com.amazonaws.services.s3.model.GetObjectRequest
        import com.amazonaws.auth.BasicAWSCredentials
        
        val fileName = "/tmp/GeoLiteCity.dat"
        val dbFile = new File(fileName)
        if (!dbFile.exists) {
            System.out.println("downloading geoip db from s3")
            val auth = new BasicAWSCredentials(System.getenv("AWS_ACCESS_KEY"), System.getenv("AWS_SECRET_KEY"))
            val client = new AmazonS3Client(auth)
            client.getObject(new GetObjectRequest(System.getenv("AWS_S3_BUCKET_NAME"), "GeoLiteCity.dat"), dbFile)
        }
        IpGeo(dbFile=fileName, memCache=false, lruCache=1)
    }


    val myRoute =
        pathPrefix("ip" / """^[0-9\.]+$""".r) { ipv4Address =>
            pathEnd {
                get {
                    respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
                        var loc: Option[IpLocation] = None
                        for (loc2 <- ipGeo.getLocation(ipv4Address)) {
                            println(loc2.countryCode + " " + loc2.countryName + ": long=" + loc2.longitude + ", lat=" + loc2.latitude)
                            loc = Some(loc2)
                        }
                        val out: IpLocation = loc.head
                        //val out = List(1,2,3) //: IpLocation = loc.head
                        complete { out.toJson.prettyPrint }
                        //val source = """{ "some": "JSON source" }"""
                        //val jsonAst = source.asJson // or JsonParser(source)
                        //val json = jsonAst.prettyPrint // or .compactPrint
                }
            }
        }
    } ~
    pathPrefix("reverse" / """^[0-9\.]+$""".r) { ipv4Address =>
        pathEnd {
            get {
                respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
                    import java.net.InetAddress
                    complete { "{\"" + ipv4Address + "\":\"" + InetAddress.getByName(ipv4Address).getCanonicalHostName + "\"}" }
                }
            }
        }
    }
}

