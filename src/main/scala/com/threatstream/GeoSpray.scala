package com.threatstream

import akka.actor.Actor
import akka.event.Logging
import spray.routing._
import spray.http._
import MediaTypes._

import com.snowplowanalytics.maxmind.iplookups.{IpLocation, IpLookups, IpLookupResult}

import spray.json._
//import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

import IpCombined._


/** Add conversion capability from IpCombined => JSON */
object ExtendedJsonProtocol extends DefaultJsonProtocol {
    implicit val ipCombinedFormat = jsonFormat15(IpCombined.apply)
}

import ExtendedJsonProtocol._


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class GeoSprayActor extends Actor with GeoSpray {

    // the HttpService trait defines only one abstract member, which
    // connects the services environment to the enclosing actor or test
    def actorRefFactory = context

    // this actor only runs our route, but you could add
    // other things here, like request stream processing
    // or timeout handling
    def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait GeoSpray extends HttpService {

    val ipLookups = {
        import java.io.File
        import com.amazonaws.services.s3.AmazonS3Client
        import com.amazonaws.services.s3.model.GetObjectRequest
        import com.amazonaws.auth.BasicAWSCredentials

        val prefix = "/tmp/"
        val geoFile = "GeoLiteCity.dat"
        val ispFile = "GeoIPISP.dat"
        val orgFile = "GeoIPOrg.dat"

        val auth = new BasicAWSCredentials(System.getenv("AWS_ACCESS_KEY"), System.getenv("AWS_SECRET_KEY"))
        val client = new AmazonS3Client(auth)

        Seq(geoFile, ispFile, orgFile).map(fileName => {
            val dbFile = new File(prefix + fileName)
            if (!dbFile.exists) {
                System.out.println("downloading geoip db from s3: " + fileName)
                client.getObject(new GetObjectRequest(System.getenv("AWS_S3_BUCKET_NAME"), fileName), dbFile)
            }
        })
        IpLookups(geoFile=Some(prefix + geoFile), ispFile=Some(prefix + ispFile), orgFile=Some(prefix + orgFile), memCache=false, lruCache=1)
    }


    val myRoute =
        pathPrefix("ip" / """^[0-9\.]+$""".r) { ipv4Address =>
            pathEnd {
                get {
                    respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
                        val ipLookupResult = ipLookups.performLookups(ipv4Address)
                        println(ipLookupResult)
                        val out: IpCombined = ipLookupResult
                        complete { out.toJson.prettyPrint }
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
    } ~
    path("") {
        get {
            complete {
                "Hello there!"
            }
        }
    }
}

