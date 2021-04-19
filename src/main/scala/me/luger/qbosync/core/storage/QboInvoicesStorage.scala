package me.luger.qbosync.core.storage

import com.redis.RedisClient
import me.luger.qbosync.core.{ IdToken, InvoiceJsonString, QboToken, RealmId }

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

sealed trait QboInvoicesStorage {
  def getInvoices(): Future[List[InvoiceJsonString]]

  def findInvoice(invoiceId: String, userId: String): Future[Option[InvoiceJsonString]]

  def saveInvoice(invoiceId: String, userId: String, invoiceJsonString: InvoiceJsonString): Future[InvoiceJsonString]

  def getInvoicesByRealm(realmId: RealmId): Future[List[InvoiceJsonString]]
}

class InMemoryQboInvoicesStorage extends QboInvoicesStorage {

  private var state: collection.concurrent.Map[String, InvoiceJsonString] =
    new ConcurrentHashMap[String, InvoiceJsonString]().asScala

  override def findInvoice(invoiceId: String, userId: String): Future[Option[InvoiceJsonString]] =
    Future.successful {
      state.get(s"$userId:$invoiceId")
    }

  override def saveInvoice(invoiceId: String,
                           userId: String,
                           invoiceJsonString: InvoiceJsonString): Future[InvoiceJsonString] =
    Future.successful {
      state.put(s"$userId:$invoiceId", invoiceJsonString)
      invoiceJsonString
    }

  override def getInvoices(): Future[List[InvoiceJsonString]] =
    Future.successful {
      state.values.toList
    }

  override def getInvoicesByRealm(realmId: RealmId): Future[List[InvoiceJsonString]] = Future.successful {
    state.keys.filter(_.startsWith(realmId)).filter(state.contains(_)).map(state(_)).toList
  }
}

class RedisQboInvoicesStorage(redisClient: RedisClient)(implicit executionContext: ExecutionContext)
    extends QboInvoicesStorage {
  import me.luger.qbosync.core.codecs._
  import io.circe._, io.circe.parser._

  override def findInvoice(invoiceId: String, userId: String): Future[Option[InvoiceJsonString]] = Future {
    redisClient
      .get[String](s"invoice:$userId:$invoiceId")
  }

  override def saveInvoice(invoiceId: String,
                           userId: String,
                           invoiceJsonString: InvoiceJsonString): Future[InvoiceJsonString] = Future {
    val key = s"invoice:$userId:${invoiceId}"
    redisClient.set(key, invoiceJsonString)
    invoiceJsonString
  }

  override def getInvoices(): Future[List[InvoiceJsonString]] =
    Future {
      redisClient
        .pipeline { p =>
          p.keys("invoice:*").map(keys => keys.map(key => p.get(key).get)).getOrElse(List())
        }
        .map(_.map(_.asInstanceOf[String]))
        .getOrElse(List())
    }

  override def getInvoicesByRealm(realmId: RealmId): Future[List[InvoiceJsonString]] =
    Future {
      redisClient
        .pipeline { p =>
          p.keys(s"invoice:${realmId}*").map(keys => keys.map(key => p.get(key).get)).getOrElse(List())
        }
        .map(_.map(_.asInstanceOf[String]))
        .getOrElse(List())
    }
}
