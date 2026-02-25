package tickets4sale.stagekeeper

import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.implicits.*
import tickets4sale.stagekeeper.domain.*
import tickets4sale.stagekeeper.routes.StagekeeperRoutes
import tickets4sale.stagekeeper.services.InventoryService

import java.time.LocalDate

class StagekeeperRoutesSpec extends CatsEffectSuite:

  private val openingDay = LocalDate.of(2026, 1, 1)
  private val testShow = Show("Cats", openingDay, Genre.Musical)
  private val inventory = Inventory(List(testShow))

  private val routesIO = for {
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

    routesIO.flatMap(_.run(request)).flatMap { response =>
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

    routesIO.flatMap(_.run(request)).flatMap { response =>
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

    routesIO.flatMap(_.run(request)).flatMap { response =>
      assertEquals(response.status, Status.Ok)

      response.as[Json].map { actualJson =>
        assertEquals(actualJson, parse(expectedJson).getOrElse(Json.Null))
      }
    }
  }

  test("GET /inventory/{date} returns 404/Empty for malformed date.") {
    val request = Request[IO](Method.GET, uri"/inventory/not-a-date")

    routesIO.flatMap(_.run(request)).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }

  test("POST /inventory/order returns 400 when overbooking") {
    val overbookJson =
      """
        {
          "show": "Cats",
          "performance_date": "2026-01-01",
          "tickets": 101
        }
      """
    val request = Request[IO](Method.POST, uri"/inventory/order")
      .withEntity(parse(overbookJson).getOrElse(Json.Null))

    routesIO.flatMap(_.run(request)).map { response =>
      assertEquals(response.status, Status.BadRequest)
    }
  }

  test("POST /inventory/order - success updates tickets_available") {
    val orderJson =
      """
      {
        "show": "Cats",
        "performance_date": "2026-01-01",
        "tickets": 10
      }
    """

    val expectedResponse =
      """
      {
        "status": "success",
        "show": "Cats",
        "performance_date": "2026-01-01",
        "tickets_bought": 10,
        "tickets_available": 90
      }
    """

    val request = Request[IO](Method.POST, uri"/inventory/order")
      .withEntity(parse(orderJson).getOrElse(Json.Null))

    routesIO.flatMap(_.run(request)).flatMap { response =>
      assertEquals(response.status, Status.Ok)
      response.as[Json].map { actualJson =>
        assertEquals(actualJson, parse(expectedResponse).getOrElse(Json.Null))
      }
    }
  }

  test("POST /inventory/order - returns 404 for non-existent show") {
    val ghostShowJson =
      """
      {
        "show": "The Phantom Show",
        "performance_date": "2026-01-01",
        "tickets": 5
      }
    """

    val request = Request[IO](Method.POST, uri"/inventory/order")
      .withEntity(parse(ghostShowJson).getOrElse(Json.Null))

    routesIO.flatMap(_.run(request)).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }

  test("Sale via POST reduces tickets available in GET overview") {
    val showTitle = "Cats"
    val date = "2026-01-01"

    val buyTicketsJson =
      s"""
      {
        "show": "$showTitle",
        "performance_date": "$date",
        "tickets": 20
      }
    """

    for {
      routes <- routesIO

      buyReq = Request[IO](Method.POST, uri"/inventory/order")
        .withEntity(parse(buyTicketsJson).getOrElse(Json.Null))
      _ <- routes.run(buyReq)

      getReq = Request[IO](Method.GET, Uri.unsafeFromString(s"/inventory/$date"))
      getResponse <- routes.run(getReq)
      inventoryJson <- getResponse.as[io.circe.Json]

      ticketsRemaining = inventoryJson.hcursor
        .downField("inventory")
        .downArray
        .downField("shows")
        .downArray
        .downField("tickets_available")
        .as[Int]
        .getOrElse(0)

    } yield {
      assertEquals(ticketsRemaining, 80)
    }
  }
