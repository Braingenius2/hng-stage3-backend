# Insighta Labs+ Backend (HNG Stage 3)

The core intelligence engine for the Insighta Labs+ platform. This backend provides secure, versioned, and rate-limited API access to profile intelligence data.

## Key Features
- **API Versioning**: Strict enforcement of `X-API-Version: 1`.
- **Security**: Stateless JWT authentication with GitHub OAuth 2.0.
- **RBAC**: Role-Based Access Control (Admin vs Analyst).
- **Rate Limiting**: Intelligent throttling using Bucket4j (10 req/min for Auth, 60 req/min for Core API).
- **Pagination**: Enterprise-grade standardized response shapes with HATEOAS-style links.
- **Natural Language Search**: Query-to-filter parsing for analysts.
- **CSV Export**: High-performance data export for reporting.

## Technology Stack
- Java 21 & Spring Boot 3.4.0
- Spring Security (Stateless JWT)
- Spring Data JPA (H2/PostgreSQL)
- Bucket4j (Rate Limiting)
- Apache Commons CSV (Export)

## Setup & Run
1. **Prerequisites**: Java 21+ installed.
2. **Environment Variables**:
   - `GITHUB_CLIENT_ID`: Your GitHub OAuth Client ID.
   - `GITHUB_CLIENT_SECRET`: Your GitHub OAuth Client Secret.
   - `JWT_SECRET`: A secure base64 string for token signing.
3. **Run**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **API Base**: `http://localhost:8081`

## API Documentation

### Required Headers
All requests to `/api/**` and `/auth/**` must include:
- `X-API-Version: 1`
- `Authorization: Bearer <token>` (except for auth endpoints)

### Authentication
- `POST /auth/github/callback`: Exchange GitHub code for JWT tokens.
- `POST /auth/refresh`: Refresh expired access tokens.
- `POST /auth/logout`: Revoke refresh tokens.

### Profiles API
- `GET /api/profiles`: List with filters (`gender`, `age_group`, `country_id`, `min_age`, `max_age`).
- `GET /api/profiles/search`: Natural language search (e.g., `?q=young males from nigeria`).
- `GET /api/profiles/export`: Export profiles to CSV.
- `POST /api/profiles`: Create new profile (**Admin only**).
- `DELETE /api/profiles/{id}`: Delete profile (**Admin only**).

### Response Shape (Pagination)
```json
{
  "data": [...],
  "meta": {
    "page": 1,
    "size": 10,
    "total_elements": 2026,
    "total_pages": 203,
    "links": {
      "self": "/api/profiles?page=1&size=10",
      "next": "/api/profiles?page=2&size=10"
    }
  }
}
```

## Compliance Notes
- Rate limiting headers (`X-Rate-Limit-Remaining`, `X-Rate-Limit-Retry-After-Seconds`) are returned.
- API versioning errors return `400 Bad Request` with code `unsupported_api_version`.
- All errors follow the `{ "status": "error", "message": "..." }` format.
