package tickets4sale.stagekeeper.services

import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import tickets4sale.stagekeeper.domain.*

import java.time.LocalDate

class InventoryService[F[_]: Sync](inventory: Inventory, sales: Ref[F, Map[(Show, LocalDate), Int]]):

  def getAvailability(date: LocalDate): F[InventoryResponse] =
    sales.get.map { currentlySold =>
      val activeShows = inventory.shows.filter(_.isRunningOn(date))

      val groups = activeShows
        .groupBy(_.genre)
        .map { case (genre, shows) =>
          GenreGroup(
            genre = genre.toString.toLowerCase,
            shows = shows.map { show =>
              val soldCount = currentlySold.getOrElse((show, date), 0)

              ShowView(
                title = show.title.toLowerCase,
                tickets_available = 100 - soldCount,
                price = show.priceOn(date)
              )
            }
          )
        }
        .toList

      InventoryResponse(groups)
    }

  def sellTickets(req: OrderRequest): F[Either[OrderResponse, OrderResponse]] =
    val maybeShow = inventory.shows.find(_.title.equalsIgnoreCase(req.show))

    maybeShow match
      case None =>
        Left(OrderResponse("failure", req.show, req.performance_date, message = Some("Show not found"))).pure[F]

      case Some(show) =>
        sales.modify { currentSales =>
          val soldSoFar = currentSales.getOrElse((show, req.performance_date), 0)
          val available = 100 - soldSoFar

          if req.tickets <= available then
            val newSales = currentSales + ((show, req.performance_date) -> (soldSoFar + req.tickets))
            val success = OrderResponse(
              "success",
              show.title,
              req.performance_date,
              Some(req.tickets),
              Some(available - req.tickets)
            )
            (newSales, Right(success))
          else
            val failure = OrderResponse(
              "failure",
              show.title,
              req.performance_date,
              message = Some(s"Ordered ${req.tickets} tickets, but only $available available")
            )
            (currentSales, Left(failure))
        }

object InventoryService:
  def create[F[_]: Sync](inventory: Inventory): F[InventoryService[F]] =
    Ref.of[F, Map[(Show, LocalDate), Int]](Map.empty).map(new InventoryService[F](inventory, _))
