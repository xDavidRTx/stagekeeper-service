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

object InventoryService:
  def create[F[_]: Sync](inventory: Inventory): F[InventoryService[F]] =
    Ref.of[F, Map[(Show, LocalDate), Int]](Map.empty).map(new InventoryService[F](inventory, _))
