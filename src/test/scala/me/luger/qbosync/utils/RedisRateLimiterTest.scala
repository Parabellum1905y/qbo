package me.luger.qbosync.utils

import com.github.sebruck.EmbeddedRedis
import com.redis.RedisClient
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.DurationInt

class RedisRateLimiterTest extends AnyFunSuite with EmbeddedRedis {
  test("test redis rate limiter") {
    withRedis() { port =>
      val redisClient      = new RedisClient("localhost", port)
      val redisRateLimiter = new RedisRateLimiter(redisClient)
      val first            = redisRateLimiter.tryAcquire("id1", 3, 5.seconds)
      assert(first)
      for (i <- 1 to 10) redisRateLimiter.tryAcquire("id1", 3, 5.seconds)
      val second = redisRateLimiter.tryAcquire("id1", 3, 5.seconds)
      assert(!second)
      Thread.sleep(5000) //5 sec
      val third = redisRateLimiter.tryAcquire("id1", 3, 5.seconds)
      assert(third)
    }
  }
}
