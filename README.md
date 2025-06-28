# TheDome backend

This project uses Ktor with MongoDB. The service periodically pulls Rust servers from the Battlemetrics API, saves them into MongoDB and exposes `/servers` endpoint which returns servers sorted by rank with pagination metadata. It also provides `/filters/options` for querying available filter values and exposes `/metrics` using Ktor's Micrometer plugin. Scheduled tasks are managed using the Ktor Task Scheduling plugin. Dependencies are wired using the Koin library.

## Running

Ensure MongoDB is accessible and set `MONGODB_URI` if needed. You can also set `PORT` to change the listening port (defaults to `8080`). The application creates the necessary MongoDB indexes on startup. Then start the server:

```
./gradlew run
```

Environment variables:

Values can also be specified in `src/main/resources/application.conf` under the `ktor.config` section.

- `MONGODB_URI` – MongoDB connection string
- `FETCH_CRON` – cron expression for server fetch schedule (defaults to every 10 minutes; uses seconds as the first field)
- `CLEANUP_CRON` – cron expression for removing outdated servers (defaults to daily at midnight)
- `API_KEY` – optional RustMaps API key
- `PORT` – overrides the default port if implemented
- `JWT_SECRET` – HMAC secret for signing tokens (**required**; the application fails if missing)
- `JWT_AUDIENCE` – JWT audience (default `thedomeAudience`)
- `JWT_ISSUER` – JWT issuer (default `thedomeIssuer`)
- `JWT_REALM` – authentication realm (default `thedomeRealm`)
- `ALLOWED_ORIGINS` – comma-separated list of allowed CORS origins (useful in production)
- `ANON_RATE_LIMIT` – requests per minute allowed for anonymous users (default `60`)
- `ANON_REFILL_PERIOD` – seconds per anonymous rate limit window (default `60`)
- `FAVOURITES_LIMIT` – maximum favourites for non-subscribers and anonymous users (default `10`)
- `SUBSCRIPTIONS_LIMIT` – maximum subscriptions for non-subscribers and anonymous users (default `10`)
- `TOKEN_VALIDITY` – access token lifetime in seconds (default `3600`)
- `ANON_TOKEN_VALIDITY` – anonymous token lifetime in seconds (default `3600`)
- `NOTIFICATION_CRON` – cron expression for wipe notification schedule (default `0 * * * *`)
- `NOTIFY_BEFORE_WIPE` – comma-separated minutes before a wipe to send notifications (e.g. `1440,60,0`)
- `NOTIFY_BEFORE_MAP_WIPE` – comma-separated minutes before a map wipe to send notifications
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
- `filter` – limit results (`ALL`, `FAVOURITES`, `SUBSCRIBED`; defaults to `ALL`)
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


Anonymous tokens are rate limited. By default they may perform `60` requests every `60` seconds. Both the limit and the period are configurable via the `ANON_RATE_LIMIT` and `ANON_REFILL_PERIOD` environment variables. Anonymous tokens cannot be refreshed. To convert an anonymous user into a registered account without losing data, send the anonymous token to `/auth/upgrade` with new credentials:

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

Use the returned `accessToken` in the `Authorization` header (e.g. `Bearer <token>`) when calling `/servers` or `/filters/options`. When you register or log in, a `refreshToken` is also returned and can be sent to `/auth/refresh` to obtain new tokens or `/auth/logout` to invalidate it. The access token also contains your username and email for convenience.


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
Battlemetrics. Recent additions include `average_fps`, `pve`, `website`,
`is_premium`, and `is_favorite` which indicate the average frames per
second, PvE status, server homepage, premium status, and whether the
server is one of your favourites.

## Favourites

Authenticated users can manage a list of favourite servers. Non-subscribers
and anonymous users may only store up to `FAVOURITES_LIMIT` servers.

```
GET    /favourites            # list favourite servers (paged)
POST   /favourites/{id}       # add server to favourites
DELETE /favourites/{id}       # remove server from favourites
```

## Subscriptions

Users can subscribe to servers for wipe notifications. Non-subscribers and
anonymous users may only follow up to `SUBSCRIPTIONS_LIMIT` servers.

```
GET    /subscriptions        # list subscribed server IDs
POST   /subscriptions/{id}   # subscribe to a server
DELETE /subscriptions/{id}   # unsubscribe from a server
```

## Docker

Build the application distribution and image:

```bash
./gradlew installDist
docker build -t thedome .
```

Run the container exposing port 8080 (set `JWT_SECRET` to start):

```bash
docker run -e JWT_SECRET=supersecret -p 8080:8080 thedome
```

## Docker Compose

You can run the project and its MongoDB dependency using Docker Compose. The provided `compose.yaml` sets up both services and handles networking and environment variables for you.

### Requirements
- Docker and Docker Compose installed
- The application is built using Eclipse Temurin JDK 21 (Alpine base image)
- MongoDB 7 is included as a service in the compose file (image `mongo:7`) to avoid unexpected upgrades

### Environment Variables
- `MONGODB_URI` – MongoDB connection string (defaults to `mongodb://mongo:27017/thedome` for the container)
- `FETCH_CRON` – cron expression for server fetch schedule (optional)
- `CLEANUP_CRON` – cron expression for server cleanup schedule (optional)
- `API_KEY` – optional RustMaps API key (optional)
- `PORT` – application port (defaults to `8080`)
- `TOKEN_VALIDITY` – access token lifetime in seconds (default `3600`)
- `ANON_TOKEN_VALIDITY` – anonymous token lifetime in seconds (default `3600`)

You can set these in the `compose.yaml` or via an `.env` file.

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

You can run the backend and MongoDB together using Docker Compose. The setup uses Eclipse Temurin JDK 21 (Alpine) for the application and the MongoDB 7 image. The application container is named `kotlin-thedome` and MongoDB is named `mongo`.

- The application exposes port `8080` (mapped to host `8080`).
- MongoDB exposes port `27017` (mapped to host `27017`).
- MongoDB data is persisted in the `mongo-data` Docker volume.
- The default MongoDB URI inside the container is `mongodb://mongo:27017/thedome`.
- You can override environment variables such as `MONGODB_URI`, `FETCH_CRON`, `CLEANUP_CRON`, `API_KEY`, and `PORT` in the `compose.yaml` or via an `.env` file.

To start everything:

```bash
docker compose up --build
```

The backend will be available at `http://localhost:8080/servers`.
You can retrieve filter options at `http://localhost:8080/filters/options`.

## Development

Coding style is defined by the `.editorconfig` file at the repository root. Configure
your editor to respect these settings when contributing.

## Running Tests

Run the test suite using the Gradle wrapper:

```bash
./gradlew test
```

All tests should pass before submitting changes.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
