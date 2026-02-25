package tickets4sale.stagekeeper

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import tickets4sale.stagekeeper.routes.StagekeeperRoutes
import tickets4sale.stagekeeper.services.{CsvInventoryLoader, InventoryService}

object StagekeeperServer:

  def run[F[_]: Async: Network]: F[Unit] =
    for
      inventory <- CsvInventoryLoader.load[F]("shows-25_26.csv")
      inventoryService <- InventoryService.create[F](inventory)

      httpApp =
        StagekeeperRoutes.routes[F](inventoryService).orNotFound

      _ <- EmberServerBuilder
        .default[F]
        .withHttpApp(httpApp)
        .build
        .useForever
    yield ()
