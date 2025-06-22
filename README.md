# TheDome backend

This project uses Ktor with MongoDB. The service periodically pulls Rust servers from the Battlemetrics API, saves them into MongoDB and exposes `/servers` endpoint which returns servers sorted by rank with pagination metadata. It also provides `/filters/options` for querying available filter values. Scheduled tasks are managed using the Ktor Task Scheduling plugin. Dependencies are wired using the Koin library.

## Running

Ensure MongoDB is accessible and set `MONGODB_URI` if needed. You can also set `PORT` to change the listening port (defaults to `8080`). The application creates the necessary MongoDB indexes on startup. Then start the server:

```
./gradlew run
```

Environment variables:

- `MONGODB_URI` – MongoDB connection string
- `FETCH_CRON` – cron expression for server fetch schedule (defaults to every 10 minutes; uses seconds as the first field)
- `API_KEY` – optional RustMaps API key
- `PORT` – overrides the default port if implemented
- `JWT_SECRET` – HMAC secret for signing tokens (default `secret`)
- `JWT_AUDIENCE` – JWT audience (default `thedomeAudience`)
- `JWT_ISSUER` – JWT issuer (default `thedomeIssuer`)
- `JWT_REALM` – authentication realm (default `thedomeRealm`)
- `ALLOWED_ORIGINS` – comma-separated list of allowed CORS origins (useful in production)
- Unhandled exceptions are logged via Ktor's `StatusPages` plugin

Query servers with optional filtering. Alongside pagination (`page` and `size`),
the following parameters can be used:

- `map` – map type
- `flag` – country code
- `region` – server region
- `difficulty` – server difficulty
- `modded` – modded servers only when `true`
- `official` – official servers only when `true`
- `wipeSchedule` – wipe schedule
- `ranking` – server rank
- `playerCount` – min player count
- `groupLimit` – maximum group limit
- `order` – ordering field (`WIPE`, `RANK`, `PLAYER_COUNT`; defaults to `WIPE`)
- `name` – substring match on server name
- `blueprints` – blueprint availability
- `kits` – kits availability
- `decay` – decay rate
- `upkeep` – upkeep cost
- `rates` – gather rate
- `seed` – world seed
- `mapSize` – map size
- `monuments` – monument count

Authentication is handled via JWT. Anonymous users can obtain a short-lived access token:

```bash
curl -X POST http://localhost:8080/auth/anonymous
```


Anonymous tokens are rate limited to 60 requests per minute and cannot be refreshed. To convert an anonymous user into a registered account without losing data, send the anonymous token to `/auth/upgrade` with new credentials:

```bash
curl -X POST http://localhost:8080/auth/upgrade \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

You can also register an account directly:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

Use the returned `accessToken` in the `Authorization` header (e.g. `Bearer <token>`) when calling `/servers` or `/filters/options`. When you register or log in, a `refreshToken` is also returned and can be sent to `/auth/refresh` to obtain new tokens or `/auth/logout` to invalidate it.


Example request:

```
GET http://localhost:8080/servers?page=1&size=20&region=EUROPE&order=PLAYER_COUNT&name=official
```

The response includes the requested page of servers and pagination fields:

```
{
  "page": 1,
  "size": 20,
  "total_pages": 5,
  "total_items": 100,
  "servers": [ ... ]
}
```

Each server in the `servers` array exposes various attributes collected from
Battlemetrics. Recent additions include `average_fps`, `pve`, `website`, and
`is_premium` which indicate the average frames per second, PvE status, server
homepage, and premium status respectively.

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
Filter options can be fetched from `http://localhost:8080/filters/options`.

### Ports
- Application: `8080` (exposed on host)
- MongoDB: `27017` (exposed for local development)

### Data Persistence
MongoDB data is persisted in a Docker volume named `mongo-data`.

## Running with Docker

You can run the backend and MongoDB together using Docker Compose. The setup uses Eclipse Temurin JDK 21 (Alpine) for the application and the latest MongoDB image. The application container is named `kotlin-thedome` and MongoDB is named `mongo`.

- The application exposes port `8080` (mapped to host `8080`).
- MongoDB exposes port `27017` (mapped to host `27017`).
- MongoDB data is persisted in the `mongo-data` Docker volume.
- The default MongoDB URI inside the container is `mongodb://mongo:27017/thedome`.
- You can override environment variables such as `MONGODB_URI`, `FETCH_CRON`, `API_KEY`, and `PORT` in the `docker-compose.yml` or via an `.env` file.

To start everything:

```bash
docker compose up --build
```

The backend will be available at `http://localhost:8080/servers`.
You can retrieve filter options at `http://localhost:8080/filters/options`.

## Development

Coding style is defined by the `.editorconfig` file at the repository root. Configure
your editor to respect these settings when contributing.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
