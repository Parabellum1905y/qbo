package me.luger.qbosync.core.storage

import com.redis.RedisClient
import io.circe.syntax.EncoderOps
import me.luger.qbosync.core.{ IdToken, QboToken, RealmId }

import scala.concurrent.{ ExecutionContext, Future }

sealed trait QboTokenStorage {

  def findTokenInfo(realmId: RealmId, idToken: IdToken): Future[Option[QboToken]]

  def saveTokenInfo(realmId: RealmId, token: QboToken): Future[QboToken]

  def getAllTokens(): Future[List[QboToken]]

  def getAllTokensByRealmId(realmId: RealmId): Future[List[QboToken]]
}

class InMemoryQboTokenStorage extends QboTokenStorage {

  private var state: Seq[QboToken] = Nil

  override def saveTokenInfo(realmId: RealmId, token: QboToken): Future[QboToken] =
    Future.successful {
      state = state :+ token
      token
    }

  override def findTokenInfo(realmId: RealmId, idToken: IdToken): Future[Option[QboToken]] =
    Future.successful(state.find(d => d.idToken == idToken))

  override def getAllTokens(): Future[List[QboToken]] = Future.successful(state.toList)

  def getAllTokensByRealmId(realmId: RealmId): Future[List[QboToken]] = Future.successful(state.toList)

}

class RedisQboTokenStorage(redisClient: RedisClient)(implicit executionContext: ExecutionContext)
    extends QboTokenStorage {
  import io.circe.parser._
  import me.luger.qbosync.core.codecs._

  override def findTokenInfo(realmId: RealmId, idToken: IdToken): Future[Option[QboToken]] =
    Future {
      redisClient
        .get[String](s"qboToken:${realmId}:$idToken")
        .flatMap { token =>
          decode[QboToken](token).toOption
        }
    }

  override def saveTokenInfo(realmId: RealmId, token: QboToken): Future[QboToken] =
    Future {
      redisClient
        .pipeline { p =>
          p.set(s"qboToken:${realmId}:${token.idToken}", token.asJson.noSpaces)
          p.expire(s"qboToken:${realmId}:${token.idToken}", token.expiresIn.toInt)
        }
        .map(_ => token)
        .head
    }

  override def getAllTokens(): Future[List[QboToken]] =
    Future {
      redisClient
        .pipeline { p =>
          p.keys("qboToken:*").map(keys => keys.map(key => p.get(key).get)).getOrElse(List())
        }
        .map(_.flatMap { token =>
          decode[QboToken](token.toString).toOption
        })
        .getOrElse(List())
    }

  override def getAllTokensByRealmId(realmId: RealmId): Future[List[QboToken]] = Future {
    redisClient
      .pipeline { p =>
        p.keys(s"qboToken:$realmId:*").map(keys => keys.map(key => p.get(key).get)).getOrElse(List())
      }
      .map(_.flatMap { token =>
        decode[QboToken](token.toString).toOption
      })
      .getOrElse(List())
  }

}
