# BakeFlow

Production and inventory management platform for bakeries and small food manufacturers.

BakeFlow is an open-source foundation for reliable stock and production operations. The current release provides the Inventory Catalog: items, batches, hierarchical locations, and optional public product lookup by barcode.

## Stack

- Angular 22, TypeScript, SCSS, PrimeNG, and PrimeIcons
- Spring Boot 4, Java 21, Maven, and Flyway
- PostgreSQL and Redis
- Docker Compose and pgAdmin

## Architecture

The backend is a modular monolith. Inventory follows DDD and SOLID boundaries: controllers invoke application services, domain objects enforce catalog rules, and persistence and external APIs remain infrastructure details.

```text
Angular
   ↓
REST API
   ↓
Spring Boot
   ↓
Application
   ↓
Domain
   ↓
Infrastructure
   ├── PostgreSQL
   └── Open Food Facts
```

Open Food Facts is isolated behind `ProductInformationGateway`. Its response is mapped to a small BakeFlow-owned contract; timeouts or service failures never prevent manual item registration. Redis caching is intentionally deferred.

## Running locally

Requirements: Docker and Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

The example values are development-only. Change them in your local `.env` when appropriate; `.env` is not versioned.

| Service | URL |
| --- | --- |
| BakeFlow | http://localhost:4300 |
| Backend health | http://localhost:8090/actuator/health |
| pgAdmin | http://localhost:5060 |

Connect pgAdmin to host `postgres`, port `5432`, using the PostgreSQL values from `.env`. The `dev` profile loads a small fictional catalog; production does not load demo data.

### Development commands

```bash
cd frontend
npm ci
npm start
npm run lint
npm test -- --watch=false
npm run build
```

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

The Angular development server proxies `/api` to `localhost:8090`. The containerized frontend proxies the same path directly to the backend service.

## API

Versioned endpoints are available under `/api/v1/items`, `/api/v1/batches`, and `/api/v1/locations`. Resources support paginated filters, create/update, and activation/deactivation without hard deletion. Barcode lookup is available at `/api/v1/product-information/barcode/{barcode}`.

## Configuration

Spring profiles are available for `dev`, `test`, and `prod`. Runtime database, Redis, and Open Food Facts settings come from environment variables. Hibernate validates the schema; Flyway owns schema evolution. Actuator exposes only `health` and `info`, with health details hidden.

## Roadmap

- [x] Inventory Catalog
- [x] Items
- [x] Batches
- [x] Hierarchical Locations
- [x] Open Food Facts integration
- [ ] Stock Balance
- [ ] Stock Movements
- [ ] FEFO
- [ ] Recipes
- [ ] Production Orders
- [ ] Authentication & RBAC
- [ ] Audit Trail
- [ ] Redis caching
- [ ] Observability

## License

Licensed under the [MIT License](LICENSE).
