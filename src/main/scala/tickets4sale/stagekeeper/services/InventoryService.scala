package tickets4sale.stagekeeper.services

import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import tickets4sale.stagekeeper.domain.*

import java.time.LocalDate

// TODO - We don't actually need to store shows from the past, but I don't verify dates on the rest of the code.
class InventoryService[F[_]: Sync](shows: Map[String, Show], sales: Ref[F, Map[(String, LocalDate), Int]]):

  def getAvailability(date: LocalDate): F[InventoryResponse] =
    sales.get.map { currentlySold =>
      // O(N) for all calls I think we can make it clever by grouping and storing once, but this is fine for
      // this use case with only a few shows
      val activeShows = shows.values.filter(_.isRunningOn(date)).toList

      val groups = activeShows
        .groupBy(_.genre)
        .map { case (genre, shows) =>
          GenreGroup(
            // On the example it was lowercased but that doesn't make much sense....
            genre = genre.toString.toLowerCase,
            shows = shows.map { show =>
              val soldCount = currentlySold.getOrElse((show.title, date), 0)

              ShowView(
                title = show.title.toLowerCase,
                tickets_available = show.initialAvailableTickets - soldCount,
                price = show.priceOn(date)
              )
            }
          )
        }
        .toList

      InventoryResponse(groups)
    }

  def sellTickets(req: OrderRequest): F[Either[FailedOrderResponse, OrderResponse]] =
    shows.get(req.show) match
      case None =>
        Left(FailedOrderResponse("failure", req.show, req.performance_date, message = "Show not found")).pure[F]

      case Some(show) if (!show.isRunningOn(req.performance_date)) =>
        Left(FailedOrderResponse("failure", req.show, req.performance_date, message = "Show not found")).pure[F]

      case Some(show) =>
        sales.modify { currentSales =>
          val soldSoFar = currentSales.getOrElse((show.title, req.performance_date), 0)
          val available = show.initialAvailableTickets - soldSoFar

          if req.tickets <= available then
            val newSales = currentSales + ((show.title, req.performance_date) -> (soldSoFar + req.tickets))
            val success = OrderResponse(
              "success",
              show.title,
              req.performance_date,
              req.tickets,
              available - req.tickets
            )
            (newSales, Right(success))
          else
            val failure = FailedOrderResponse(
              "failure",
              show.title,
              req.performance_date,
              message = s"Ordered ${req.tickets} tickets, but only $available available"
            )
            (currentSales, Left(failure))
        }

object InventoryService:
  def create[F[_]: Sync](shows: Map[String, Show]): F[InventoryService[F]] =
    Ref.of[F, Map[(String, LocalDate), Int]](Map.empty).map(new InventoryService[F](shows, _))
