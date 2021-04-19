package me.luger.qbosync.utils

import akka.actor.Actor
import me.luger.qbosync.core
import me.luger.qbosync.core.{ AuthData, AuthTokenContent, RealmId }
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
  def receive = {
    case Run => {
      for {
        auth <- authDataStorage.getAll()
        tokens: Seq[(RealmId, List[core.QboToken])] <- {
          Future.sequence(auth.map(x => qboTokenStorage.getAllTokensByRealmId(x.realmId).map(t => x.realmId -> t)))
        }
        _ <- {
          val a = tokens.map {
            case (realmId, tokens) =>
              tokens.headOption match {
                case None =>
                case Some(token) =>
                  invoiceService.pageInvoices(AuthTokenContent(realmId, token.idToken, 0), AuthData(realmId), token)
              }
          }
          Future.successful()
        } //invoiceService.loadInvoice()
      } yield tokens

    } //Do something
  }
}
