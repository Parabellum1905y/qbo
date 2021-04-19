package me.luger.qbosync.utils

import pureconfig.generic.ProductHint
import pureconfig.{ CamelCase, ConfigFieldMapping, ConfigSource }
import pureconfig.generic.auto._
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.auto._

import scala.concurrent.duration.{ Duration, DurationInt }
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Success

case class Config(secretKey: String,
                  http: HttpConfig,
                  database: DatabaseConfig,
                  qboconfig: QBOConfig,
                  redis: RedisConfig)

case class IntuitResponse(authorization_endpoint: String,
                          token_endpoint: String,
                          issuer: String,
                          jwks_uri: String,
                          revocation_endpoint: String,
                          userinfo_endpoint: String)

object Config {
  val sttpBackend      = AkkaHttpBackend()
  implicit def hint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  def load()(implicit executionContext: ExecutionContext) =
    ConfigSource.default.load[Config] match {
      case Right(config) =>
        val QboAuthClient = new QboAuthClient(config)
        val response = Await
          .result(QboAuthClient.retrieveApiParams(), Duration.Inf)
        config.copy(
          qboconfig = config.qboconfig
            .copy(
              qbocred = config.qboconfig.qbocred.copy(intuitAuthorizationEndpoint =
                                                        Some(response.authorization_endpoint),
                                                      intuitBearerTokenEndpoint = Some(response.token_endpoint))
            )
        )
      case Left(error) =>
        throw new RuntimeException("Cannot read config file, errors:\n" + error.toList.mkString("\n"))
    }
}

private[utils] case class HttpConfig(host: String, port: Int)
private[utils] case class DatabaseConfig(jdbcUrl: String, username: String, password: String)
private[utils] case class QBOConfig(qboBaseUrl: String, qbocred: QBOCred, qbolimits: QBOLimits)
private[utils] case class QBOCred(clientId: String,
                                  clientSecret: String,
                                  appRedirectUri: String,
                                  discoveryAPIHost: String,
                                  intuitAuthorizationEndpoint: Option[String],
                                  intuitBearerTokenEndpoint: Option[String],
                                  c2qbScope: String,
                                  siwiScope: String,
                                  appNowScope: String)
private[utils] case class QBOLimits(
    apiRequestsLimit: Int,
    batchRequestsLimit: Int,
    batchPayloadLimit: Int,
    duration: Int
)

case class RedisConfig(host: String, port: Int)
