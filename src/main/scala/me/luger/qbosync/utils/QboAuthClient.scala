package me.luger.qbosync.utils

import io.circe.generic.auto._
import io.circe.{ DecodingFailure, Json }
import me.luger.qbosync.core.service.QboAuthService.BearerTokenResponse
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.circe.{ asJson, asJsonAlways }
import sttp.client3.{ basicRequest, Request, Response, UriContext }

import java.net.URLEncoder
import java.util.Base64
import scala.concurrent.{ ExecutionContext, Future }

class QboAuthClient(config: Config)(implicit executionContext: ExecutionContext) {
  val sttpBackend = AkkaHttpBackend()

  def retrieveBearerToken(authCode: String, qbocred: QBOCred): Future[Either[DecodingFailure, BearerTokenResponse]] = {
    import me.luger.qbosync.core.service.QboAuthService._
    basicRequest
      .post(uri"${qbocred.intuitBearerTokenEndpoint.getOrElse("")}")
      .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
      .header("Accept", "application/json")
      .header(
        "Authorization",
        s"Basic ${Base64.getEncoder.encodeToString(s"${qbocred.clientId}:${qbocred.clientSecret}".getBytes)}"
      )
      .body(
        Map("code" -> authCode, "redirect_uri" -> qbocred.appRedirectUri, "grant_type" -> "authorization_code")
      )
      .response(asJsonAlways[Json].getRight)
      .send(sttpBackend)
      .map { resp =>
        decoder.decodeJson(resp.body)
      }
  }

  def retrieveApiParams(): Future[IntuitResponse] = {
    val request: Request[IntuitResponse, Any] = basicRequest
      .get(uri"${config.qboconfig.qbocred.discoveryAPIHost}")
      .response(asJson[IntuitResponse].getRight)
    request
      .send(sttpBackend)
      .map { resp: Response[IntuitResponse] =>
        resp.body
      }
  }

  def prepareQboUrl(state: String): String = {
    val creds = config.qboconfig.qbocred
    s"${creds.intuitAuthorizationEndpoint.getOrElse("")}?client_id=${creds.clientId}&response_type=code&scope=${URLEncoder
      .encode(creds.appNowScope, "UTF-8")}&redirect_uri=${URLEncoder.encode(creds.appRedirectUri, "UTF-8")}&state=$state"
  }

}
