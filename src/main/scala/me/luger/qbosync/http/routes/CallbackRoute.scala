package me.luger.qbosync.http.routes

import akka.http.scaladsl.model.StatusCodes.{ OK, ServiceUnavailable }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import me.luger.qbosync.core.service.QboAuthService
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

class CallbackRoute(authService: QboAuthService)(implicit executionContext: ExecutionContext)
    extends FailFastCirceSupport {

  val route: Route =
    path("oauth2redirect") {
      parameters("code".as[String], "state".as[String], "realmId".as[String]).as(QBOResponse.apply _) { qboResponse =>
        complete(
          authService.signUp(qboResponse.code, qboResponse.realmId).map {
            case Left(e) => {
              println(s"error:$e")
              ServiceUnavailable -> e.message.asJson
            }
            case Right(token) => OK -> token.asJson
          }
        )
      }
    }
}

private case class QBOResponse(code: String, state: String, realmId: String)
