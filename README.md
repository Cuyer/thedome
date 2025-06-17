# TheDome backend

This project uses Ktor with MongoDB. The service periodically pulls Rust servers from the Battlemetrics API, saves them into MongoDB and exposes `/servers` endpoint which returns servers sorted by rank with pagination. Scheduled tasks are managed using the Ktor Task Scheduling plugin.

## Running

Ensure MongoDB is accessible and set `MONGODB_URI` if needed. You can also set `PORT` to change the listening port (defaults to `8080`). Then start the server:

```
./gradlew run
```

Environment variables:

- `MONGODB_URI` – MongoDB connection string
- `FETCH_CRON` – cron expression for server fetch schedule (defaults to hourly)
- `API_KEY` – optional RustMaps API key
- `PORT` – overrides the default port if implemented
- Unhandled exceptions are logged via Ktor's `StatusPages` plugin

Query servers with optional `page` and `size` parameters:

```
GET http://localhost:8080/servers?page=1&size=20
```

## Docker

Build the application distribution and image:

```bash
./gradlew installDist
docker build -t thedome .
```

Run the container exposing port 8080:

```bash
docker run -p 8080:8080 thedome
```

## Docker Compose

You can run the project and its MongoDB dependency using Docker Compose. The provided `docker-compose.yml` sets up both services and handles networking and environment variables for you.

### Requirements
- Docker and Docker Compose installed
- The application is built using Eclipse Temurin JDK 21 (Alpine base image)
- MongoDB is included as a service in the compose file

### Environment Variables
- `MONGODB_URI` – MongoDB connection string (defaults to `mongodb://mongo:27017/thedome` for the container)
- `FETCH_CRON` – cron expression for server fetch schedule (optional)
- `API_KEY` – optional RustMaps API key (optional)
- `PORT` – application port (defaults to `8080`)

You can set these in the `docker-compose.yml` or via an `.env` file.

### Build and Run

To build and start the services:

```bash
docker compose up --build
```

This will start two containers:
- `kotlin-thedome` (the application) on port `8080`
- `mongo` (MongoDB) on port `27017`

The application will be available at `http://localhost:8080/servers`.

### Ports
- Application: `8080` (exposed on host)
- MongoDB: `27017` (exposed for local development)

### Data Persistence
MongoDB data is persisted in a Docker volume named `mongo-data`.

## Development

Coding style is defined by the `.editorconfig` file at the repository root. Configure
your editor to respect these settings when contributing.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
