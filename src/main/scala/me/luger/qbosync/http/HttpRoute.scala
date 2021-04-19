package me.luger.qbosync.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import me.luger.qbosync.core.service.{ QboAuthService, QboInvoiceService }
import me.luger.qbosync.http.routes.{ CallbackRoute, QBOAuthRoute, QboInvoiceRoute }
import me.luger.qbosync.utils.QboAuthClient

import scala.concurrent.ExecutionContext

class HttpRoute(
    qboAuthService: QboAuthService,
    qboInvoiceService: QboInvoiceService,
    secretKey: String,
    QboAuthClient: QboAuthClient
)(implicit executionContext: ExecutionContext) {
  private val qboAuthRouter     = new QBOAuthRoute(QboAuthClient)
  private val qboCallbackRouter = new CallbackRoute(qboAuthService)
  private val invoicesRouter    = new QboInvoiceRoute(secretKey, qboInvoiceService)

  val route: Route =
    cors() {
      pathPrefix("v1") {
        qboAuthRouter.route ~
        qboCallbackRouter.route ~
        invoicesRouter.route
      } ~
      pathPrefix("healthcheck") {
        get {
          complete("OK")
        }
      }
    }

}
