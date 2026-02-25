package tickets4sale.stagekeeper

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  val run: IO[Unit] = StagekeeperServer.run[IO]
