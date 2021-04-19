package me.luger.qbosync.core.service

import com.roundeights.hasher.Implicits._
import io.circe.{ Decoder, DecodingFailure, HCursor }
import me.luger.qbosync.core.storage.{ AuthDataStorage, QboTokenStorage }
import me.luger.qbosync.core.{ AuthData, AuthToken, AuthTokenContent, IdToken, QboToken, RealmId }
import me.luger.qbosync.utils.{ Config, QboAuthClient }
import me.luger.qbosync.utils.MonadTransformers._
import io.circe.generic.auto._
import io.circe.syntax._
import pdi.jwt.{ Jwt, JwtAlgorithm }

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

class QboAuthService(
    qboTokenStorage: QboTokenStorage,
    authDataStorage: AuthDataStorage,
    config: Config,
    QboAuthClient: QboAuthClient
)(implicit executionContext: ExecutionContext) {
  def signUp(authCode: String, realmId: RealmId): Future[Either[DecodingFailure, AuthToken]] =
    for {
      token: Either[DecodingFailure, QboAuthService.BearerTokenResponse] <- QboAuthClient.retrieveBearerToken(
        authCode,
        config.qboconfig.qbocred
      )
      authData <- authDataStorage
        .saveAuthData(AuthData(realmId))
      encodedToken <- token
        .map { validToken =>
          qboTokenStorage
            .saveTokenInfo(realmId, validToken.compact())
            .map(savedToken => encodeToken(authData.realmId, savedToken.idToken, savedToken.expiresIn))
        }
        .foldT()
    } yield encodedToken

  private def encodeToken(userId: RealmId, idToken: IdToken, expiresIn: Long): AuthToken =
    Jwt.encode(AuthTokenContent(userId, idToken, System.currentTimeMillis() + expiresIn * 1000).asJson.noSpaces,
               config.secretKey,
               JwtAlgorithm.HS256)

}
object QboAuthService {
  val decoderDerived: Decoder[BearerTokenResponse] = deriveDecoder
  val decoderCamelSnake: Decoder[BearerTokenResponse] = (c: HCursor) =>
    for {
      expiresIn              <- c.downField("expires_in").as[Long]
      idToken                <- c.downField("id_token").as[String]
      refreshToken           <- c.downField("refresh_token").as[String]
      xRefreshTokenExpiresIn <- c.downField("x_refresh_token_expires_in").as[Long]
      accessToken            <- c.downField("access_token").as[String]
      tokenType              <- c.downField("token_type").as[String]
    } yield {
      BearerTokenResponse(
        expiresIn,
        idToken,
        refreshToken,
        xRefreshTokenExpiresIn,
        accessToken,
        tokenType
      )
  }

  implicit val decoder: Decoder[BearerTokenResponse] = decoderDerived.or(decoderCamelSnake)
  final case class BearerTokenResponse(
      expiresIn: Long,
      idToken: AuthToken,
      refreshToken: String,
      xRefreshTokenExpiresIn: Long,
      accessToken: String,
      tokenType: String
  )

  implicit class ToQboToken(bearerTokenResponse: BearerTokenResponse) {
    def compact(): QboToken = {
      import bearerTokenResponse._
      QboToken(expiresIn, idToken, refreshToken, xRefreshTokenExpiresIn, accessToken, tokenType)
    }
  }
}
