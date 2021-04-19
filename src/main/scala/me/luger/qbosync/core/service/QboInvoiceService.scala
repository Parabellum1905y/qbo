package me.luger.qbosync.core.service

import com.typesafe.scalalogging.Logger
import io.circe.Json
import me.luger.qbosync.core.storage.{ AuthDataStorage, QboInvoicesStorage, QboTokenStorage }
import me.luger.qbosync.core.{ AuthData, AuthTokenContent, InvoiceJsonString, InvoiceTemplate, QboToken }
import me.luger.qbosync.utils.{ Config, QboInvoiceClient, RedisRateLimiter }

import scala.concurrent.{ ExecutionContext, Future }

class QboInvoiceService(
    qboInvoicesStorage: QboInvoicesStorage,
    authDataStorage: AuthDataStorage,
    qboTokenStorage: QboTokenStorage,
    config: Config,
    redisRateLimiter: RedisRateLimiter
)(implicit executionContext: ExecutionContext) {
  val logger = Logger(classOf[QboInvoiceService])

  def getInvoices(): Future[Seq[InvoiceJsonString]] =
    qboInvoicesStorage.getInvoices()

  def create(secret: AuthTokenContent, invoiceTemplate: InvoiceTemplate): Future[Option[Json]] =
    for {
      authData <- authDataStorage.findAuthData(secret.realmId)
      qboToken <- qboTokenStorage.findTokenInfo(secret.realmId, secret.idToken)
      saved <- (authData, qboToken) match {
        case (Some(auth), Some(token)) =>
          val qboInvoiceClient = QboInvoiceClient(config, auth, token, redisRateLimiter)
          qboInvoiceClient.create(invoiceTemplate)
        case _ => Future.successful(None)
      }
    } yield saved

  def getInvoice(secret: AuthTokenContent, invoiceId: String): Future[Option[InvoiceJsonString]] =
    for {
      invoice <- qboInvoicesStorage.findInvoice(invoiceId, secret.realmId)
      invoiceJsonStr <- invoice match {
        case None =>
          println(s"invoice didn't found locally for : ${secret.realmId}:$invoiceId")
          for {
            authData <- authDataStorage.findAuthData(secret.realmId)
            qboToken <- qboTokenStorage.findTokenInfo(secret.realmId, secret.idToken)
            invoiceJsonString <- {
              (authData, qboToken) match {
                case (Some(auth), Some(token)) => loadInvoice(secret, auth, token, invoiceId)
                case _                         => Future.successful(None)
              }
            }
          } yield invoiceJsonString
        case Some(x) =>
          logger.info(s"invoce : ${x}")
          Future.successful(Option(x))
      }
    } yield invoiceJsonStr

  def loadInvoice(secret: AuthTokenContent,
                  authData: AuthData,
                  qboToken: QboToken,
                  invoiceId: String): Future[Option[InvoiceJsonString]] = {
    val qboInvoiceClient = QboInvoiceClient(config, authData, qboToken, redisRateLimiter)
    qboInvoiceClient
      .read(invoiceId)
      .flatMap(
        invoice =>
          invoice.flatMap(inv => inv.findAllByKey("Id").map(_.toString().replaceAll("\"", "")).headOption) match {
            case None => Future.successful(None)
            case Some(invoiceId) =>
              qboInvoicesStorage
                .saveInvoice(invoiceId, secret.realmId, invoice.get.toString())
                .map(Option(_))
        }
      )
  }

  def pageInvoices(secret: AuthTokenContent,
                   authData: AuthData,
                   qboToken: QboToken): Future[List[InvoiceJsonString]] = {
    val qboInvoiceClient = QboInvoiceClient(config, authData, qboToken, redisRateLimiter)
    qboInvoiceClient
      .pageWithQuery(0)
      .flatMap {
        case List() => Future.successful(List())
        case xInv =>
          Future.sequence(
            xInv
              .map { x: Json =>
                x.findAllByKey("Id").map(_.toString().replaceAll("\"", "")).headOption -> x
              }
              .filter { case (x, y) => x.isEmpty }
              .map {
                case (Some(id), inv) =>
                  qboInvoicesStorage
                    .saveInvoice(id, secret.realmId, inv.noSpaces)
              }
          )
      }
  }

}
