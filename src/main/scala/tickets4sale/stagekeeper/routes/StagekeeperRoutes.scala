package tickets4sale.stagekeeper.routes

import cats.data.OptionT
import cats.syntax.all.*
import cats.effect.Sync
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder.*
import tickets4sale.stagekeeper.services.InventoryService

import java.time.LocalDate
import scala.util.Try

object StagekeeperRoutes:
  def routes[F[_]: Sync](service: InventoryService[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "inventory" / dateStr =>
      val maybeDate = Try(LocalDate.parse(dateStr)).toOption

      maybeDate
        .traverse { date =>
          for
            response <- service.getAvailability(date)
            result <- Ok(response)
          yield result
        }
        .flatMap {
          case Some(resp) => resp.pure[F]
          case None       => OptionT.none[F, Response[F]].value.map(_.getOrElse(Response(Status.NotFound)))
        }
    }
