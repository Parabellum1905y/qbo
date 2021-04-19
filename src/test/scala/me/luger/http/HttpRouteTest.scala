package me.luger.http

import akka.http.scaladsl.server.Route
import me.luger.BaseServiceTest
import me.luger.qbosync.core.service.{ QboAuthService, QboInvoiceService }
import me.luger.qbosync.http.HttpRoute
import me.luger.qbosync.utils.QboAuthClient

class HttpRouteTest extends BaseServiceTest {

  "HttpRoute" when {

    "GET /healthcheck" should {

      "return 200 OK" in new Context {
        Get("/healthcheck") ~> httpRoute ~> check {
          responseAs[String] shouldBe "OK"
          status.intValue() shouldBe 200
        }
      }

    }

  }

  trait Context {
    val secretKey                            = "secret"
    val authService: QboAuthService          = mock[QboAuthService]
    val qboInvoiceService: QboInvoiceService = mock[QboInvoiceService]
    val QboAuthClient: QboAuthClient         = mock[QboAuthClient]
    val httpRoute: Route =
      new HttpRoute(authService, qboInvoiceService, secretKey, QboAuthClient).route
  }

}
