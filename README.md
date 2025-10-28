# Country Currency & Exchange API (Spring Boot)

## Overview
Fetches countries from https://restcountries.com, fetches exchange rates from https://open.er-api.com, computes an estimated_gdp, caches countries in MySQL, and provides REST endpoints.

## Endpoints
- POST /countries/refresh
- GET /countries?region=Africa&currency=NGN&sort=gdp_desc&page=0&size=50
- GET /countries/{name}
- DELETE /countries/{name}
- GET /status
- GET /countries/image

## Requirements
- Java 17+
- Maven
- MySQL (or provide `JDBC_DATABASE_URL` for Railway)

## Run locally
1. Create MySQL DB (e.g., `countrydb`) and user.
2. Set env vars:
    - `JDBC_DATABASE_URL=jdbc:mysql://localhost:3306/countrydb`
    - `DB_USER=root`
    - `DB_PASS=yourpass`
    - `PORT=8080` (optional)
3. Build and run: use
   - mvn clean package or 
   - java -jar target/country-exchange-api-0.0.1-SNAPSHOT.jar

4. Trigger refresh: use Postmam:
curl -X POST http://localhost:8080/countries/refresh


## Deploy to Railway
1. Create a new Railway project and add a MySQL plugin. Railway will provide a `JDBC_DATABASE_URL`.
2. In Railway, set environment variables:
- `JDBC_DATABASE_URL` (provided)
- `DB_USER` (if not embedded in JDBC URL)
- `DB_PASS` (if not embedded)
3. Add your repository and set build command `mvn -DskipTests package` and start command `java -jar target/country-exchange-api-0.0.1-SNAPSHOT.jar`.
4. Deploy.

