package me.luger.qbosync.utils

import com.redis.RedisClient
import me.luger.qbosync.core.RealmId

import scala.concurrent.duration.Duration

class RedisRateLimiter(redisClient: RedisClient) {

  def tryAcquire(userId: String, limit: Int, duration: Duration): Boolean = {
    val key    = keyByUserId(userId)
    val before = redisClient.get(key)
    val permits = redisClient.pipeline { p =>
      before match {
        case null | None =>
          p.setex(key, duration.toSeconds.toInt, limit)
        case Some(_) =>
      }
      p.decr(key)
    }
    permits.getOrElse(List()).last.asInstanceOf[Option[Long]].getOrElse(-1L) >= 0
  }

  private def keyByUserId(userId: RealmId): String = s"limit:${userId}"

}
