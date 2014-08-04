package com.threatstream

import java.io.File
import akka.actor.Actor
import akka.event.Logging
import spray.routing._
import spray.http._
import MediaTypes._

import com.maxmind.geoip.LookupService
import com.snowplowanalytics.maxmind.iplookups.{IpLocation, IpLookups, IpLookupResult}

import spray.json._
//import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

import IpCombined._


/** Add conversion capability from IpCombined => JSON */
object ExtendedJsonProtocol extends DefaultJsonProtocol {
    implicit val ipCombinedFormat = jsonFormat16(IpCombined.apply)
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

    private val _prefix = System.getenv("HOME") + "/"

    val ipLookups = {
        import java.io.File
        import com.amazonaws.services.s3.AmazonS3Client
        import com.amazonaws.services.s3.model.GetObjectRequest
        import com.amazonaws.auth.BasicAWSCredentials

        val geoFile = "GeoLiteCity.dat"
        val netspeedFile = "GeoIPNetspeed.dat"
        val orgFile = "GeoIPOrg.dat"
        val asnFile = "GeoIPASNum.dat"

        Seq(geoFile, netspeedFile, orgFile, asnFile).map(fileName => {
            val dbFile = new File(_prefix + fileName)
            if (!dbFile.exists) {
                throw new Exception("Missing required data file: " + dbFile.getAbsolutePath)
            }
        })
        IpLookups(geoFile=Some(_prefix + geoFile), netspeedFile=Some(_prefix + netspeedFile), orgFile=Some(_prefix + orgFile), memCache=false, lruCache=1)
    }

    val asnLookups = {
        val asnFile = "GeoIPASNum.dat"
        val dbFile = new File(_prefix + asnFile)
        if (!dbFile.exists) {
            throw new Exception("Missing required data file: " + dbFile.getAbsolutePath)
        }
        new LookupService(_prefix + asnFile)
    }


    val myRoute =
        pathPrefix("ip" / """^[0-9\.]+$""".r) { ipv4Address =>
            pathEnd {
                get {
                    respondWithMediaType(`application/json`) { // XML is marshalled to `text/xml` by default, so we simply override here
                        val ipLookupResult = ipLookups.performLookups(ipv4Address)
                        println(ipLookupResult)
                        val out: IpCombined = (ipLookupResult, asnLookups.getOrg(ipv4Address))
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

