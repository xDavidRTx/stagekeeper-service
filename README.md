# Tickets4Sale: Stagekeeper 🎟️

A functional, non-blocking HTTP API built with **Scala 3**, **http4s**, and **Cats Effect**.

Stagekeeper loads theater show inventories from a CSV file into a thread-safe, in-memory state (`cats.effect.Ref`) and serves that data via REST endpoints.


## How to Run

1. Clone the repository and navigate into the project folder:
   ```bash
   git clone git@github.com:xDavidRTx/stagekeeper-service.git
   cd stagekeeper-service
   ```
2. Start the server using sbt:
    ```bash
   sbt run
   ```
3. Wait until you see the http4s Ember server startup log indicating it is listening on `127.0.0.1:8080`

## EndPoints

### GET /inventory/{date}
Fetches the available inventory for a specific date.

**Example Context**
Considering the following inventory:

| Title            | Opening day | Genre   |
| :--------------- | :---------- | :------ |
| Cats             | 2023-01-01  | musical |
| Comedy of Errors | 2023-07-01  | comedy  |
| Everyman         | 2023-08-01  | drama   |

**Request**
```http
curl -X GET "http://127.0.0.1:8080/inventory/2026-02-15" -H "accept: application/json"
```

**Response**
```json
{
  "inventory": [
    {
      "genre": "comedy",
      "shows": [
        {
          "title": "comedy of errors",
          "tickets_available": 100,
          "price": 50
        }
      ]
    },
    {
      "genre": "drama",
      "shows": [
        {
          "title": "everyman",
          "tickets_available": 100,
          "price": 40
        }
      ]
    }
  ]
}
```

### POST /inventory/order

Places an order for tickets for a specific show and performance date. This endpoint is strictly transactional and prevents overbooking.

Request Body
The request must be a JSON object containing the show title, the target date, and the number of tickets requested.

```json
{
  "show": "Cats",
  "performance_date": "2026-01-01",
  "tickets": 10
}
```

#### Success Response
Status Code: `200 OK`

Condition: Sufficient tickets are available and the show exists.

Body:

```json
{
   "status": "success",
   "show": "Cats",
   "performance_date": "2026-01-01",
   "tickets_bought": 10,
   "tickets_available": 90
}
```

#### Failure Response (Overbooking)
Status Code: 400 Bad Request

Condition: The number of requested tickets exceeds the remaining capacity.

Body:

```json
{
   "status": "failure",
   "show": "Cats",
   "performance_date": "2026-01-01",
   "message": "Ordered 10 tickets, but only 5 available"
}
```

Failure Response (Not Found)
Status Code: 404 Not Found

Condition: The show title provided does not exist in the inventory.

Body:

```json
{
  "status": "failure",
  "show": "Unknown Show",
  "performance_date": "2026-01-01",
  "message": "Show not found"
}
```

**Example Request**
```http
curl -X POST "http://localhost:8080/inventory/order" \
     -H "Content-Type: application/json" \
     -d '{
       "show": "Phantom of the Opera",
       "performance_date": "2026-01-01",
       "tickets": 5
     }'
```
