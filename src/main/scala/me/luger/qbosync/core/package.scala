package me.luger.qbosync

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

package object core {

  type RealmId           = String
  type AuthToken         = String
  type IdToken           = String
  type InvoiceJsonString = String

  final case class AuthTokenContent(realmId: RealmId, idToken: IdToken, expireAt: Long)

  final case class QboToken(
      expiresIn: Long,
      idToken: AuthToken,
      refreshToken: String,
      xRefreshTokenExpiresIn: Long,
      accessToken: String,
      tokenType: String
  )

  final case class AuthData(realmId: RealmId) {
    require(realmId.nonEmpty, "realmId.empty")
  }

  final case class InvoiceTemplate(Line: List[Line], CustomerRef: CustomerRef, CurrencyRef: Option[CurrencyRef])

  final case class Line(DetailType: String, Amount: BigDecimal, SalesItemLineDetail: Option[SalesItemLine])

  final case class CustomerRef(value: String, name: Option[String])

  final case class CurrencyRef(value: String, name: Option[String])

  final case class SalesItemLine(ItemRef: Option[ItemRef])

  final case class ItemRef(value: String, name: Option[String])

  object codecs {
    implicit val authDataDecoder: Decoder[AuthData]                 = deriveDecoder
    implicit val authTokenContentDecoder: Decoder[AuthTokenContent] = deriveDecoder
    implicit val qboTokenDecoder: Decoder[QboToken]                 = deriveDecoder

    implicit val authDataEncoder: Encoder[AuthData]                 = deriveEncoder
    implicit val authTokenContentEncoder: Encoder[AuthTokenContent] = deriveEncoder
    implicit val qboTokenEncoder: Encoder[QboToken]                 = deriveEncoder
    val formatter                                                   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    implicit val dateEncoder                                        = Encoder.encodeString.contramap[LocalDateTime](_.format(formatter))
    implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
      Try(LocalDateTime.parse(str, formatter)).toEither.left.map(e => e.getMessage)
    })

  }
}
