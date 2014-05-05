package com.threatstream

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor
  val service = system.actorOf(Props[GeoSprayActor], "geospray-service")

  val listenPort: Int = {
        Option(System.getenv("PORT")) match {
            case Some(port) => port.toInt
            case None => 8080
        }
  }

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = listenPort)
}

