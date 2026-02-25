package tickets4sale.stagekeeper

import cats.effect.IO
import io.circe.Json
import org.http4s.*
import org.http4s.implicits.*
import io.circe.parser.parse
import org.http4s.circe.CirceEntityDecoder.*
import munit.CatsEffectSuite
import tickets4sale.stagekeeper.domain.*
import tickets4sale.stagekeeper.services.InventoryService

import java.time.LocalDate
import tickets4sale.stagekeeper.routes.StagekeeperRoutes

class StagekeeperRoutesSpec extends CatsEffectSuite:

  private val openingDay = LocalDate.of(2026, 1, 1)
  private val testShow = Show("Cats", openingDay, Genre.Musical)
  private val inventory = Inventory(List(testShow))

  private val routes = for {
    service <- InventoryService.create[IO](inventory)
  } yield StagekeeperRoutes.routes[IO](service).orNotFound

  test("GET /inventory/{date} returns exact expected JSON structure") {
    val requestDate = "2026-01-01"
    val request = Request[IO](Method.GET, Uri.unsafeFromString(s"/inventory/$requestDate"))

    val expectedJson =
      """
        {
          "inventory": [
            {
              "genre": "musical",
              "shows": [
                {
                  "title": "cats",
                  "tickets_available": 100,
                  "price": 70
                }
              ]
            }
          ]
        }
      """

    routes.flatMap(_.run(request)).flatMap { response =>
      assertEquals(response.status, Status.Ok)

      response.as[Json].map { actualJson =>
        assertEquals(actualJson, parse(expectedJson).getOrElse(Json.Null))
      }
    }
  }

  test("GET /inventory/{date} applies 20% discount after 80 days.") {
    val discountDate = "2026-03-22"
    val request = Request[IO](Method.GET, Uri.unsafeFromString(s"/inventory/$discountDate"))

    val expectedJson =
      """
      {
        "inventory": [
          {
            "genre": "musical",
            "shows": [
              {
                "title": "cats",
                "tickets_available": 100,
                "price": 56
              }
            ]
          }
        ]
      }
    """

    routes.flatMap(_.run(request)).flatMap { response =>
      assertEquals(response.status, Status.Ok)

      response.as[Json].map { actualJson =>
        assertEquals(
          actualJson,
          parse(expectedJson).getOrElse(Json.Null)
        )
      }
    }
  }

  test("GET /inventory/{date} returns 0 shows past the 100 days windonw.") {
    val lateDate = "2026-06-01"
    val request = Request[IO](Method.GET, Uri.unsafeFromString(s"/inventory/$lateDate"))

    val expectedJson = """{"inventory" : [] }"""

    routes.flatMap(_.run(request)).flatMap { response =>
      assertEquals(response.status, Status.Ok)

      response.as[Json].map { actualJson =>
        assertEquals(actualJson, parse(expectedJson).getOrElse(Json.Null))
      }
    }
  }

  test("GET /inventory/{date} returns 404/Empty for malformed date.") {
    val request = Request[IO](Method.GET, uri"/inventory/not-a-date")

    routes.flatMap(_.run(request)).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }
