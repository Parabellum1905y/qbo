package me.luger.qbosync.utils

import akka.actor.Actor
import com.typesafe.scalalogging.Logger
import me.luger.qbosync.core
import me.luger.qbosync.core.{ AuthData, AuthTokenContent, InvoiceJsonString, RealmId }
import me.luger.qbosync.core.service.QboInvoiceService
import me.luger.qbosync.core.storage.{ AuthDataStorage, QboInvoicesStorage, QboTokenStorage }
import me.luger.qbosync.utils.InvoiceUpdater.Run

import scala.concurrent.{ ExecutionContext, Future }

object InvoiceUpdater {
  sealed trait Command
  final case class Run() extends Command

  //lazy val tickActor:TickActor

}

class InvoiceUpdater(authDataStorage: AuthDataStorage,
                     qboTokenStorage: QboTokenStorage,
                     invoiceService: QboInvoiceService)(
    implicit executionContext: ExecutionContext
) extends Actor {
  val logger: Logger = Logger(classOf[InvoiceUpdater])
  def receive = {
    case Run => {
      for {
        auth <- authDataStorage.getAll()
        tokens: Seq[(RealmId, List[core.QboToken])] <- {
          Future.sequence(auth.map(x => qboTokenStorage.getAllTokensByRealmId(x.realmId).map(t => x.realmId -> t)))
        }
        loaded <- {
          Future.sequence(tokens.map {
            case (realmId, tokens) =>
              tokens.headOption match {
                case None => Future.successful(List())
                case Some(token) =>
                  invoiceService
                    .pageInvoices(AuthTokenContent(realmId, token.idToken, 0), AuthData(realmId), token)
                    .map { dbg =>
                      logger.debug(s"$dbg")
                    }
              }
          })

        } //invoiceService.loadInvoice()
      } yield loaded

    } //Do something
  }
}
