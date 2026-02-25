package tickets4sale.stagekeeper.domain

import io.circe.Encoder

final case class InventoryResponse(inventory: List[GenreGroup]) derives Encoder
