package tickets4sale.stagekeeper.domain

import io.circe.{Decoder, Encoder}

import java.time.LocalDate

final case class OrderRequest(
    show: String,
    performance_date: LocalDate,
    tickets: Int
) derives Decoder

final case class OrderResponse(
    status: String,
    show: String,
    performance_date: LocalDate,
    tickets_bought: Int,
    tickets_available: Int
) derives Encoder

final case class FailedOrderResponse(status: String, show: String, performance_date: LocalDate, message: String)
    derives Encoder
