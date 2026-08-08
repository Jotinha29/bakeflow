# BakeFlow

Production and inventory management platform for bakeries and small food manufacturers.

BakeFlow is an open-source foundation for reliable stock and production operations. The project is currently in its initial setup phase: infrastructure, application shells, health checks, and engineering conventions are ready, while business capabilities remain intentionally unimplemented.

## Stack

- Angular 22, TypeScript, SCSS, PrimeNG, and PrimeIcons
- Spring Boot 4, Java 21, Maven, and Flyway
- PostgreSQL and Redis
- Docker Compose and pgAdmin

## Architecture

The backend is a modular monolith prepared for domain-oriented modules using DDD and SOLID principles. Future modules will keep domain, application, and infrastructure concerns separate without introducing premature abstractions. The frontend separates route-level pages from reusable features and centralizes infrastructure concerns under `core`.

## Running locally

Requirements: Docker and Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

The example values are development-only. Change them in your local `.env` when appropriate; `.env` is not versioned.

| Service | URL |
| --- | --- |
| BakeFlow | http://localhost:4200 |
| Backend health | http://localhost:8080/actuator/health |
| pgAdmin | http://localhost:5050 |

Connect pgAdmin to host `postgres`, port `5432`, using the PostgreSQL values from `.env`.

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

The Angular development server proxies `/api` to `localhost:8080`. The containerized frontend proxies the same path to the backend service.

## Configuration

Spring profiles are available for `dev`, `test`, and `prod`. Runtime database and Redis settings come from environment variables. Hibernate validates the schema; Flyway owns schema evolution. Actuator exposes only `health` and `info`, with health details hidden.

## Roadmap

- [ ] Inventory Management
- [ ] Batch Traceability
- [ ] Warehouse Locations
- [ ] Stock Movements
- [ ] Recipes
- [ ] Production Orders
- [ ] FEFO
- [ ] External API Integrations (Open Food Facts, ViaCEP, Open-Meteo)
- [ ] Authentication & RBAC
- [ ] Audit Trail
- [ ] Observability

## License

Licensed under the [MIT License](LICENSE).
