package tickets4sale.stagekeeper.domain

import io.circe.Encoder

final case class Inventory(shows: List[Show])

final case class InventoryResponse(inventory: List[GenreGroup]) derives Encoder
