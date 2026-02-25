package tickets4sale.stagekeeper.domain

import io.circe.Encoder

case class GenreGroup(genre: String, shows: List[ShowView]) derives Encoder
