package tickets4sale.stagekeeper.services

import cats.effect.{Async, Sync}
import cats.syntax.all.*
import fs2.data.csv.*
import fs2.io.readInputStream
import fs2.text
import tickets4sale.stagekeeper.domain.*

import java.time.LocalDate
import scala.util.Try

object CsvInventoryLoader:

  implicit val showDecoder: CsvRowDecoder[Show, String] = CsvRowDecoder.instance { row =>
    for {
      title <- row.as[String]("title")
      dateStr <- row.as[String]("opening")
      genreStr <- row.as[String]("genre")

      date <- Try(LocalDate.parse(dateStr)).toEither
        .leftMap(e => new DecoderError(s"Invalid date format: ${e.getMessage}"))

      genre <- Genre
        .fromString(genreStr)
        .leftMap(e => new DecoderError(e))

    } yield Show(title, date, genre)
  }

  def load[F[_]: Async](path: String): F[Inventory] =
    val inputStream = Sync[F].blocking(getClass.getClassLoader.getResourceAsStream(path))

    readInputStream(inputStream, 4096)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[Show]())
      .compile
      .toList
      .map(shows => Inventory(shows))
