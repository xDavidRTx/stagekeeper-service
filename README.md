# Tickets4Sale: Stagekeeper 🎟️

A purely functional, non-blocking HTTP API built with **Scala 3**, **http4s**, and **Cats Effect**.

Stagekeeper loads theater show inventories from a CSV file into a thread-safe, in-memory state (`cats.effect.Ref`) using the **fs2** streaming library, and serves that data via REST endpoints.


## How to Run

1. Clone the repository and navigate into the project folder:
   ```bash
   git clone <your-repo-url>
   cd stagekeeper
   ```
2. Start the server using sbt:
    ```bash
   sbt run
   ```
3. Wait until you see the http4s Ember server startup log indicating it is listening on `127.0.0.1:8080`

## EndPoints

### Get Inventory by Date
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
curl -X GET "http://127.0.0.1:8080/inventory/2023-08-15" -H "accept: application/json"
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
