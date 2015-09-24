package actors

import akka.actor.{Props, ActorRef, Actor}
import play.Logger
import play.api.libs.json.Json

/**
 * Created by raymond on 2015/09/24.
 */

class TwitterStreamer(out: ActorRef) extends Actor {
  def receive = {
    case "subscribe" =>
      Logger.info("Received subscription from  a client")
      out ! Json.obj("text" -> "Hello world!")
  }
}
  object TwitterStreamer {
    def props(out: ActorRef) = Props(new TwitterStreamer(out))
  }

