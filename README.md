# TheDome backend

This project uses Ktor with MongoDB. The service periodically pulls Rust servers from the Battlemetrics API, saves them into MongoDB and exposes `/servers` endpoint which returns servers sorted by rank with pagination.

## Running

Ensure MongoDB is accessible and set `MONGODB_URI` if needed. You can also set `PORT` to change the listening port (defaults to `8080`). Then start the server:

```
./gradlew run
```

Environment variables:

- `MONGODB_URI` – MongoDB connection string
- `FETCH_DELAY_MS` – fetch interval in milliseconds
- `API_KEY` – optional RustMaps API key
- `PORT` – overrides the default port if implemented

Query servers with optional `page` and `size` parameters:

```
GET http://localhost:8080/servers?page=1&size=20
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
