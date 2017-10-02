package xyz.driver.tracing
package google

import java.nio.file._
import java.time._

import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling._
import akka.stream._
import akka.stream.scaladsl._
import pdi.jwt._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent._

object OAuth2 {

  private case class ServiceAccount(project_id: String,
                                    private_key: String,
                                    client_email: String)
  private implicit val serviceAccountFormat = jsonFormat3(ServiceAccount)

  private case class GrantResponse(access_token: String, expires_in: Int)
  private implicit val grantResponseFormat = jsonFormat2(GrantResponse)

  /** Request a new access token for the given scopes.
    *
    * Implements the OAUTH2 workflow as descried here
    * https://developers.google.com/identity/protocols/OAuth2ServiceAccount
    */
  def requestAccessToken(
      http: HttpExt,
      serviceAccountFile: Path,
      scopes: Seq[String]
  )(implicit ec: ExecutionContext,
    mat: Materializer): Future[(Instant, String)] =
    Future {
      val now = Instant.now.toEpochMilli / 1000
      val credentials =
        (new String(Files.readAllBytes(serviceAccountFile), "utf-8")).parseJson
          .convertTo[ServiceAccount]

      val claim = JwtClaim(
        issuer = Some(credentials.client_email),
        expiration = Some(now + 60 * 60),
        issuedAt = Some(now)
      ) +
        ("aud", "https://www.googleapis.com/oauth2/v4/token") +
        ("scope", scopes.mkString(" "))

      Jwt.encode(claim, credentials.private_key, JwtAlgorithm.RS256)
    } flatMap { assertion =>
      http.singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = "https://www.googleapis.com/oauth2/v4/token"
        ).withEntity(
          FormData(
            "grant_type" -> "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion" -> assertion
          ).toEntity))
    } flatMap { response =>
      Unmarshal(response).to[GrantResponse]
    } map { grant =>
      (Instant.now.plusSeconds(grant.expires_in), grant.access_token)
    }

  /** Flow that injects access tokens into a stream of HTTP requests.
    *
    * Re-authentication happens transparently when access tokens expire. Note:
    * in case an access token gets revoked, this flow needs to be restarted in
    * order to re-authenticate
    */
  def authenticatedFlow(http: HttpExt,
                        serviceAccountFile: Path,
                        scopes: Seq[String],
                        graceSeconds: Int = 300)(
      implicit ec: ExecutionContext,
      mat: Materializer): Flow[HttpRequest, HttpRequest, _] =
    Flow[HttpRequest]
      .scanAsync[(HttpRequest, Instant, String)](
        (HttpRequest(), Instant.now, "")) {
        case ((_, expiration, accessToken), request) =>
          if (Instant.now isAfter expiration.minusSeconds(graceSeconds)) {
            http.system.log.info("tracing access token expired, refreshing")
            requestAccessToken(http, serviceAccountFile, scopes).map {
              case (newExpiration, newToken) =>
                http.system.log.debug("new tracing access token otained")
                (request, newExpiration, newToken)
            }
          } else {
            Future.successful((request, expiration, accessToken))
          }
      }
      .drop(1) // drop initial element
      .map {
        case (request, _, accessToken) =>
          request.withHeaders(
            RawHeader("Authorization", "Bearer " + accessToken)
          )
      }

}
