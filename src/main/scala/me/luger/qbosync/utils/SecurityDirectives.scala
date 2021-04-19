package me.luger.qbosync.utils

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.{ BasicDirectives, HeaderDirectives, RouteDirectives }
import io.circe.generic.auto._
import io.circe.parser._
import me.luger.qbosync.core.AuthTokenContent
import pdi.jwt._

object SecurityDirectives {

  import BasicDirectives._
  import HeaderDirectives._
  import RouteDirectives._

  def authenticate(secretKey: String): Directive1[AuthTokenContent] =
    headerValueByName("Authorization")
      .map { ff =>
        Jwt.decodeRaw(ff.replace("Bearer ", ""), secretKey, Seq(JwtAlgorithm.HS256))
      }
      .map(_.toOption.flatMap(decode[AuthTokenContent](_).toOption))
      .flatMap {
        case Some(result) =>
          if (isTokenExpired(result)) reject
          else provide(result)
        case None =>
          reject
      }

  private def isTokenExpired(jwt: AuthTokenContent): Boolean =
    jwt.expireAt < System.currentTimeMillis()

}
