package controllers

import actors.TwitterStreamer.TwitterStreamer
import akka.actor.ActorRef
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json.{JsValue, JsObject, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Logger, Play}
import play.extras.iteratees._
import actors.TwitterStreamer
import scala.concurrent.Future

class Application extends Controller {

  val (iteratee, enumerator) = Concurrent.joined[Array[Byte]]

  val jsonStream: Enumerator[JsObject] =
    enumerator &>
      Encoding.decode() &>
      Enumeratee.grouped(JsonIteratees.jsSimpleObject)


  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  val loggingIteratee = Iteratee.foreach[JsObject] { value =>
    Logger.info(value.toString)
  }

  jsonStream run loggingIteratee

  def tweets = WebSocket.acceptWithActor[String, JsValue] {
    (request: RequestHeader) => (out: ActorRef) => TwitterStreamer.props(out)
  }

  def tweets_backup = Action.async {
    credentials.map { case (consumerKey, requestToken) =>
      WS
        .url("https://stream.twitter.com/1.1/statuses/filter.json")
        .sign(OAuthCalculator(consumerKey, requestToken))
        .withQueryString("track" -> "cats")
        .get {response =>
        Logger.info("Status: " + response.status)
        iteratee}
        .map { _ =>
        Ok("Stream closed")
      }
    } getOrElse {
      Future {
        InternalServerError("Twitter credentials missing")
      }
    }
  }

  def testget = Action {
    val jsonString = """{"name": "n name", "description": "new description"}"""
    val json = Json.parse(jsonString)
    Ok(json)
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