package actors

import akka.actor.{Props, ActorRef, Actor}
import play.Logger
import play.api.Play
import play.api.libs.iteratee.{Iteratee, Enumerator, Enumeratee, Concurrent}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import play.api.libs.ws.WS
import play.extras.iteratees.JsonIteratees
import play.extras.iteratees._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

/**
 * Created by raymond on 2015/09/24.
 */

class TwitterStreamer(out: ActorRef) extends Actor {
  def receive = {
    case "subscribe" =>
      Logger.info("Received subscription from a client")
      TwitterStreamer.subscribe(out)
  }
}
  object TwitterStreamer {
    def props(out: ActorRef) = Props(new TwitterStreamer(out))

    private var broadcastEnumerator: Option[Enumerator[JsObject]] = None

    def connect(): Unit = {
      credentials.map { case (consumerKey, requestToken) =>
        val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]
        val jsonStream: Enumerator[JsObject] = enumerator &>
          Encoding.decode() &>
          Enumeratee.grouped(JsonIteratees.jsSimpleObject)
        val (be, _) = Concurrent.broadcast(jsonStream)
        broadcastEnumerator = Some(be)
        val url = "https://stream.twitter.com/1.1/statuses/filter.json"
        WS
          .url(url)
          .withRequestTimeout(0)
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "cats")
          .get { response =>
          Logger.info("Status: " + response.status)
          iteratee
        }.map { _ =>
          Logger.info("Twitter stream closed")
        }
      } getOrElse {
        Logger.error("Twitter credentials missing")
      }

    }

    def subscribe(out: ActorRef): Unit = {
      if (broadcastEnumerator.isEmpty) {
        Logger.info("Connecting")
        connect()
      }
      val twitterClient = Iteratee.foreach[JsObject] { t => out ! t }
      broadcastEnumerator.map { enumerator =>
        enumerator.run(twitterClient)
      }
    }


    def credentials: Option[(ConsumerKey, RequestToken)] = for {
      apiKey <- Play.configuration.getString("twitter.apiKey")
      apiSecret <- Play.configuration.getString("twitter.apiSecret")
      token <- Play.configuration.getString("twitter.token")
      tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
    } yield (
        ConsumerKey(apiKey, apiSecret),
        RequestToken(token, tokenSecret)
        )
  }


