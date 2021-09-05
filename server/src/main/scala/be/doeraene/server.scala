package be.doeraene

import akka.actor.typed.{ActorSystem, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import be.doeraene.data.*
import be.doeraene.models.GameState
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{head as _, *}
import akka.stream.OverflowStrategy
import akka.stream.typed.scaladsl.ActorSource
import io.circe.generic.auto.*
import io.circe.syntax.*
import scalatags.Text.all.*
import scalatags.Text

import java.awt.{MouseInfo, Robot}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.io.StdIn

@main def printMousePos(): Unit = {
  Thread.sleep(3000)
  val mousePosition = MouseInfo.getPointerInfo.getLocation
  println((mousePosition.x, mousePosition.y))
}

@main def runServer(topLeftX: Int, topLeftY: Int): Unit = {

  given ToEntityMarshaller[Text.TypedTag[String]] =
    summon[ToEntityMarshaller[String]]
      .compose[Text.TypedTag[String]]("<!DOCTYPE html>" + _)
      .map(_.withContentType(ContentTypes.`text/html(UTF-8)`))

  extension [T, M](source: Source[T, M])
    def distinct: Source[T, M] = source
      .scan((Option.empty[T], false))((acc, t) =>
        acc._1 match {
          case None           => (Some(t), true) // initial state, value changed by default
          case Some(oldValue) => (Some(t), oldValue != t)
        }
      )
      .collect { case (Some(t), true) => t } // collecting actual changes

  println(s"Top left square is ${(topLeftX, topLeftY)}")
  val wowGameStateProvider = WOWGameStateProvider((topLeftX, topLeftY))

  implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.ignore[Any], "Server")
  given ExecutionContext = actorSystem.executionContext

  val gameStateDistributor = actorSystem.systemActorOf(
    GameStateDistributor(),
    "GameStateDistributor"
  )

  val gameStatePipeline = wowGameStateProvider
    .sourceLogErrorAndContinue(every = 3000.millis)
    .distinct
    .to(GameStateDistributor.sink(gameStateDistributor))

  val playerActor = actorSystem.systemActorOf(
    Behaviors.setup[PlayerActor.Command] { context =>
      gameStateDistributor ! GameStateDistributor.Subscribe(PlayerActor.Update(_), context.self)
      PlayerActor()
    },
    "PlayerActor"
  )

  val host = "0.0.0.0"
  val port = 9000

  val bindingFuture = Http()
    .newServerAt(host, port)
    .bind(
      get {
        path("ws" / "follow-game-state") {
          val (outActor, publisher) = ActorSource
            .actorRef[GameState](
              { case _ if false => }: PartialFunction[GameState, Unit],
              { case e: Any if false => new Exception(e.toString) }: PartialFunction[Any, Throwable],
              10,
              OverflowStrategy.dropTail
            )
            .toMat(Sink.asPublisher(false))(Keep.both)
            .run()
          actorSystem.systemActorOf(
            Behaviors.setup[GameState] { context =>
              context.log.info("New WS connection.")
              gameStateDistributor ! GameStateDistributor.Subscribe(identity, context.self)
              context.watch(outActor)
              Behaviors
                .receiveMessage { (gameState: GameState) =>
                  outActor ! gameState
                  Behaviors.same
                }
                .receiveSignal { case (_, Terminated(_)) =>
                  Behaviors.stopped(() => println(s"Actor ${context.self} stopped."))
                }
            },
            "Client-" ++ java.util.UUID.randomUUID().toString
          )
          handleWebSocketMessages(
            Flow.fromSinkAndSource(
              Flow[Message].to(Sink.ignore),
              Source.fromPublisher(publisher).map(_.asJson.noSpaces).map(TextMessage(_))
            )
          )
        } ~
          path("main.js") { getFromResource("main.js") } ~
          complete(StatusCodes.OK, html(body(div(h1("Wow robot")), script(src := "/main.js"))))
      }
    )

  Future {
    gameStatePipeline.run()
  }

  println(s"Server online at http://$host:$port/")
  println("Press RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => actorSystem.terminate()) // and shutdown when done

}
