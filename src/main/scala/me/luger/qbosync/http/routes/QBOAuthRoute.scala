package me.luger.qbosync.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import me.luger.qbosync.core.service.QboAuthService
import me.luger.qbosync.utils.QboAuthClient

import java.util.UUID
import scala.concurrent.ExecutionContext

class QBOAuthRoute(QboAuthClient: QboAuthClient)(implicit executionContext: ExecutionContext)
    extends FailFastCirceSupport {

  import StatusCodes._

  val route = pathPrefix("qbo") {
    path("connect") {
      pathEndOrSingleSlash {
        get {
          redirect(QboAuthClient.prepareQboUrl(UUID.randomUUID().toString), SeeOther)
        }
      }
    }
  }

}
