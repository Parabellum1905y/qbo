package me.luger.qbosync.utils

import com.typesafe.scalalogging.Logger
import io.circe
import io.circe.Json
import me.luger.qbosync.core.{ AuthData, InvoiceTemplate, QboToken }
import me.luger.qbosync.utils.QboInvoiceClient.selectInvoicesForUpdate
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.circe.asJsonAlways
import sttp.client3.{ basicRequest, DeserializationException, Response, UriContext }

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }

class QboInvoiceClient(config: Config, authData: AuthData, qboToken: QboToken, redisRateLimiter: RedisRateLimiter)(
    implicit executionContext: ExecutionContext
) {
  val logger = Logger(classOf[QboInvoiceClient])

  val sttpBackend = AkkaHttpBackend()
  def create(invoiceTemplate: InvoiceTemplate): Future[Option[Json]] = {
    import io.circe.generic.auto._
    import sttp.client3.circe._
    if (redisRateLimiter.tryAcquire(authData.realmId,
                                    config.qboconfig.qbolimits.apiRequestsLimit,
                                    config.qboconfig.qbolimits.duration.seconds)) { //TODO move to service
      logger.debug("start creation of invoice")
      val request = basicRequest
        .post(uri"${config.qboconfig.qboBaseUrl}/v3/company/${authData.realmId}/invoice")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", s"Bearer ${qboToken.accessToken}")
        .body(invoiceTemplate)
        .response(asJsonAlways[Json].getRight)
      request
        .send(sttpBackend)
        .map { resp: Response[Json] =>
          Option(resp.body)
        }
        .recover { e =>
          println(e)
          logger.error(e.getMessage)
          None
        }
    } else {
      logger.debug("start creation of invoice:no allowed")
      Future.successful(None)
    }
  }

  def read(invoiceId: String): Future[Option[Json]] =
    if (redisRateLimiter.tryAcquire(authData.realmId,
                                    config.qboconfig.qbolimits.apiRequestsLimit,
                                    config.qboconfig.qbolimits.duration.seconds)) {
      val request = basicRequest
        .get(uri"${config.qboconfig.qboBaseUrl}/v3/company/${authData.realmId}/invoice/$invoiceId")
        .header("Authorization", s"Bearer ${qboToken.accessToken}")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .response(asJsonAlways[Json])
      request
        .send(sttpBackend)
        .map { resp =>
          logger.info(s"${resp.body}")
          resp.body.toOption
        }
        .recover { e =>
          println(e)
          logger.error(e.getMessage)
          None
        }
    } else Future.successful(None)

  def pageWithQuery(start: Int) =
    if (redisRateLimiter.tryAcquire(authData.realmId,
                                    config.qboconfig.qbolimits.apiRequestsLimit,
                                    config.qboconfig.qbolimits.duration.seconds)) {

      val request = basicRequest
        .get(
          uri"${config.qboconfig.qboBaseUrl}/v3/company/${authData.realmId}/query?query=${selectInvoicesForUpdate(start, config.qboconfig.qbolimits.batchRequestsLimit)}"
        )
        .header("Authorization", s"Bearer ${qboToken.accessToken}")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .response(asJsonAlways[List[Json]])
      request
        .send(sttpBackend)
        .map { resp =>
          logger.info(s"${resp.body}")
          resp.body match {
            case Left(e) =>
              logger.error(e.getMessage)
              List()
            case Right(r) =>
              r
          }
        }
    } else Future.successful(List())

}

object QboInvoiceClient {
  def selectInvoicesForUpdate(start: Int, limit: Int) = s"select * from startposition $start maxresults $limit"
  def apply(config: Config, authData: AuthData, qboToken: QboToken, redisRateLimiter: RedisRateLimiter)(
      implicit executionContext: ExecutionContext
  ) = new QboInvoiceClient(config, authData, qboToken, redisRateLimiter)
}
