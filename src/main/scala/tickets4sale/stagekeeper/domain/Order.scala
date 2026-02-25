package tickets4sale.stagekeeper.domain

import io.circe.{Codec, Decoder, Encoder}

import java.time.LocalDate

final case class OrderRequest(
    show: String,
    performance_date: LocalDate,
    tickets: Int
) derives Codec

final case class OrderResponse(
    status: String,
    show: String,
    performance_date: LocalDate,
    tickets_bought: Option[Int] = None,
    tickets_available: Option[Int] = None,
    message: Option[String] = None
)

object OrderResponse:
  given Encoder[OrderResponse] = Encoder.derived[OrderResponse].mapJson(_.dropNullValues)
