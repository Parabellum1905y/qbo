package me.luger.qbosync.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import me.luger.qbosync.core.InvoiceTemplate
import me.luger.qbosync.core.service.QboInvoiceService
import me.luger.qbosync.utils.SecurityDirectives

import scala.concurrent.ExecutionContext

class QboInvoiceRoute(
    secretKey: String,
    invoiceService: QboInvoiceService
)(implicit executionContext: ExecutionContext)
    extends FailFastCirceSupport {

  import SecurityDirectives._
  import StatusCodes._
  import invoiceService._

  val route = pathPrefix("invoices") {
    pathEndOrSingleSlash {
      authenticate(secretKey) { secret =>
        get {
          complete(getInvoices())
        }
      }
    } ~ pathPrefix(Segment) { invoiceId =>
      pathEndOrSingleSlash {
        println(s"invoiceId $invoiceId")
        authenticate(secretKey) { secret =>
          println(s"invoiceId $invoiceId ${secret.idToken}")
          get {
            complete(getInvoice(secret, invoiceId).map {
              case Some(invoice) =>
                parse(invoice) match {
                  case Left(e)    => BadRequest -> e.getMessage().asJson
                  case Right(inv) => OK         -> inv
                }
              case None => NotFound -> None.asJson
            })
          }
        }
      }
    } ~ post {
      authenticate(secretKey) { secret =>
        entity(as[InvoiceTemplate]) { invoice =>
          complete(create(secret, invoice).map {
            case Some(profile) =>
              OK -> profile.asJson
            case None =>
              BadRequest -> None.asJson
          })
        }
      }
    }

  } /*~

    pathPrefix("me") {
      pathEndOrSingleSlash {
        authenticate(secretKey) { secret =>
          get {
            complete(getProfile(secret.userId))
          } ~
          post {
            entity(as[UserProfileUpdate]) { userUpdate =>
              complete(updateProfile(secret.userId, userUpdate).map(_.asJson))
            }
          }
        }
      }
    } ~
    pathPrefix(Segment) { id =>
      pathEndOrSingleSlash {
        get {
          complete(getProfile(id).map {
            case Some(profile) =>
              OK -> profile.asJson
            case None =>
              BadRequest -> None.asJson
          })
        } ~
        post {
          entity(as[UserProfileUpdate]) { userUpdate =>
            complete(updateProfile(id, userUpdate).map {
              case Some(profile) =>
                OK -> profile.asJson
              case None =>
                BadRequest -> None.asJson
            })
          }
        }
      }
    }
 */

}
