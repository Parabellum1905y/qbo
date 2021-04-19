package me.luger.qbosync.core.storage

import com.redis.RedisClient
import io.circe.syntax.EncoderOps
import me.luger.qbosync.core.{ AuthData, RealmId }

import scala.concurrent.{ ExecutionContext, Future }

sealed trait AuthDataStorage {

  def findAuthData(realmId: RealmId): Future[Option[AuthData]]

  def saveAuthData(authData: AuthData): Future[AuthData]

  def getAll(): Future[List[AuthData]]
}

class InMemoryAuthDataStorage extends AuthDataStorage {

  private var state: Seq[AuthData] = Nil

  override def findAuthData(realmId: RealmId): Future[Option[AuthData]] =
    Future.successful(state.find(d => d.realmId == realmId))

  override def saveAuthData(authData: AuthData): Future[AuthData] =
    Future.successful {
      state = state :+ authData
      authData
    }

  override def getAll(): Future[List[AuthData]] = Future.successful { state.toList }

}

class RedisAuthDataStorage(redisClient: RedisClient)(implicit executionContext: ExecutionContext)
    extends AuthDataStorage {
  import io.circe.parser._
  import me.luger.qbosync.core.codecs._

  override def findAuthData(realmId: RealmId): Future[Option[AuthData]] =
    Future {
      redisClient
        .get[String](s"authdata:$realmId")
        .flatMap { auth =>
          decode[AuthData](auth).toOption
        }
    }

  override def saveAuthData(authData: AuthData): Future[AuthData] =
    Future {
      redisClient
        .pipeline { p =>
          p.set(s"authdata:${authData.realmId}", authData.asJson.noSpaces)
        }
        .map(_ => authData)
        .head
    }

  override def getAll(): Future[List[AuthData]] = Future {
    redisClient
      .pipeline { p =>
        p.keys("authdata:*").map(keys => keys.map(key => p.get(key).get)).getOrElse(List())
      }
      .map(_.flatMap { authData =>
        decode[AuthData](authData.toString).toOption
      })
      .getOrElse(List())
  }

}
