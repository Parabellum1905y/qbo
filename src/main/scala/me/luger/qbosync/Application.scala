package me.luger.qbosync

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import com.redis.RedisClient
import me.luger.qbosync.core.service.{ QboAuthService, QboInvoiceService }
import me.luger.qbosync.core.storage.{ RedisAuthDataStorage, RedisQboInvoicesStorage, RedisQboTokenStorage }
import me.luger.qbosync.http.HttpRoute
import me.luger.qbosync.utils.InvoiceUpdater.Run
import me.luger.qbosync.utils.{ Config, InvoiceUpdater, QboAuthClient, RedisRateLimiter }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Application extends App {

  def startApplication() = {
    implicit val actorSystem: ActorSystem   = ActorSystem()
    implicit val executor: ExecutionContext = actorSystem.dispatcher

    val config = Config.load()

    val redisClient = new RedisClient(config.redis.host, config.redis.port)

    val authDataStorage    = new RedisAuthDataStorage(redisClient)
    val qboDataStorage     = new RedisQboTokenStorage(redisClient)
    val qboInvoicesStorage = new RedisQboInvoicesStorage(redisClient)

    val redisRateLimiter = new RedisRateLimiter(redisClient)

    val QboAuthClient = new QboAuthClient(config)

    val authService = new QboAuthService(qboDataStorage, authDataStorage, config, QboAuthClient)
    val qboInvoiceService =
      new QboInvoiceService(qboInvoicesStorage, authDataStorage, qboDataStorage, config, redisRateLimiter)
    val httpRoute = new HttpRoute(authService, qboInvoiceService, config.secretKey, QboAuthClient)

    val tickActor = actorSystem.actorOf(Props(new InvoiceUpdater(authDataStorage, qboDataStorage, qboInvoiceService)))

    actorSystem.scheduler.scheduleWithFixedDelay(1.minutes, 4.minutes, tickActor, Run)

    Http().newServerAt(config.http.host, config.http.port).bind(httpRoute.route)
  }

  startApplication()

}
