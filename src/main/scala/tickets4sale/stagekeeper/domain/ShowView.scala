package tickets4sale.stagekeeper.domain

import io.circe.Encoder

final case class ShowView(title: String, tickets_available: Int, price: Int) derives Encoder
