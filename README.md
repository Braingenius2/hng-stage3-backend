# Profile Intelligence API (HNG Stage 1 + Stage 2)

This repository contains the same backend project across two milestones:
- Stage 1 delivered core profile enrichment, storage, and CRUD endpoints.
- Stage 2 upgrades the same service into a queryable intelligence engine with advanced filtering, sorting, pagination, and rule-based natural language search.

## Stack
- Java 21
- Spring Boot 3.4.0
- Spring Data JPA (Specifications)
- H2 (local) and PostgreSQL driver (runtime dependency)
- Maven
- Jackson

## Public Base URL
`https://hng-stage1-profile-production.up.railway.app`

## Run Locally
1. Ensure Java 21 is installed.
2. Start the API:
   - `./mvnw spring-boot:run` (Linux/macOS)
   - `mvnw.cmd spring-boot:run` (Windows)
3. API base URL: `http://localhost:8081`

## Project Evolution

### Stage 1 (Foundation)
- Create/enrich profiles from external APIs.
- Persist normalized profile data in the database.
- Expose core CRUD operations.

### Stage 2 (Queryable Intelligence Engine)
- Advanced multi-condition filtering on `/api/profiles`.
- Sorting with `sort_by` + `order`.
- Pagination with `page` and `limit` (max 50).
- Rule-based natural language parsing on `/api/profiles/search`.
- Standardized validation and error contracts.

## Seed Data
- Startup seeding reads `src/main/resources/seed_profiles.json` (2026 records).
- Seeding is idempotent by profile name and skips existing records.

## Endpoints

### `GET /api/profiles`
Advanced filtering, sorting, and pagination.

Supported query parameters:
- `gender`: `male | female`
- `age_group`: `child | teenager | adult | senior`
- `country_id`: 2-letter ISO code (for example `NG`, `KE`)
- `min_age`, `max_age`
- `min_gender_probability`, `min_country_probability` (0 to 1)
- `sort_by`: `age | created_at | gender_probability | country_probability`
- `order`: `asc | desc`
- `page`: default `1` (must be `>= 1`)
- `limit`: default `10`, max `50`

Response shape:
```json
{
  "status": "success",
  "page": 1,
  "limit": 10,
  "total": 2026,
  "data": []
}
```

### `GET /api/profiles/search`
Rule-based natural language query parsing with pagination.

Query params:
- `q` (required, non-empty)
- `page` (default `1`)
- `limit` (default `10`, max `50`)

Examples:
- `young males` -> `gender=male`, `min_age=16`, `max_age=24`
- `females above 30` -> `gender=female`, `min_age=30`
- `people from angola` -> `country_id=AO`
- `adult males from kenya` -> `gender=male`, `age_group=adult`, `country_id=KE`
- `male and female teenagers above 17` -> `age_group=teenager`, `min_age=17`

### Other endpoints
- `POST /api/profiles`
- `GET /api/profiles/{id}`
- `DELETE /api/profiles/{id}`

These CRUD endpoints are part of the Stage 1 baseline and remain available in Stage 2.

## Validation and Error Contract
Error response shape:
```json
{
  "status": "error",
  "message": "<error message>"
}
```

Status code behavior:
- `400`: missing/empty required parameter, invalid query parameters, or uninterpretable NL query
- `422`: invalid parameter type
- `404`: profile not found
- `500`: server failure

## Extra Compliance Notes
- CORS allows `*` origin.
- IDs are UUID v7.
- `created_at` values are UTC ISO 8601.

## Test
Run:
- `./mvnw test` or `mvnw.cmd test`
