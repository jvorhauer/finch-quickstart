package zdavep

import io.finch.{ Endpoint, HttpRequest, HttpResponse }
import io.finch.json.Json
import io.finch.json.finch._
import io.finch.request.OptionalParam

import com.twitter.finagle.http.{ Method, Status }
import com.twitter.finagle.http.path._
import com.twitter.finagle.Service

trait StatusService extends Versioned with Responder {

  def status = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest) = for {
      msg <- OptionalParam("msg")(req)
    } yield respondWith(Status.Ok)(
      Json.obj("status" -> "success", "data" -> s"${msg.getOrElse("ok")}")
    )
  }

  val statusEndpoints = new Endpoint[HttpRequest, HttpResponse] {
    def route = {
      case Method.Get -> Root / "z" / "api" / `version` / "status" => status
    }
  }

}