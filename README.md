# TheDome backend

This project uses Ktor with MongoDB. The service periodically pulls Rust servers from the Battlemetrics API, saves them into MongoDB and exposes `/servers` endpoint which returns servers sorted by rank with pagination.

## Running

Ensure MongoDB is accessible and set `MONGODB_URI` if needed. Then start the server:

```
./gradlew run
```

Query servers with optional `page` and `size` parameters:

```
GET http://localhost:8080/servers?page=1&size=20
```
