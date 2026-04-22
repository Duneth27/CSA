# Smart Campus Sensor & Room Management API

## Overview

The Smart Campus API is a RESTful web service built using JAX-RS (Jakarta RESTful Web Services) framework. It provides a comprehensive solution for managing rooms and sensors in a modern university campus infrastructure. The API enables campus facilities managers and automated building systems to interact seamlessly with campus data through well-defined HTTP endpoints.

### Key Features
- **Room Management**: Create, retrieve, update, and delete campus rooms
- **Sensor Management**: Register and manage various sensor types (Temperature, CO2, Occupancy)
- **Historical Data**: Track sensor readings over time with comprehensive reading history
- **Error Handling**: Sophisticated error handling with meaningful HTTP status codes and JSON responses
- **Request/Response Logging**: Complete observability through filter-based logging
- **Sub-Resource Pattern**: Hierarchical resource structure reflecting campus organization
- **Data Validation**: Comprehensive validation and business logic constraints

## Technology Stack

- **Framework**: JAX-RS 3.1 (Jakarta RESTful Web Services)
- **Implementation**: Jersey 3.1.1
- **Server**: Apache Tomcat 10.1+ (Servlet 6.0)
- **Language**: Java 11
- **Build Tool**: Maven 3.6+
- **Data Storage**: In-memory data structures (ConcurrentHashMap, ArrayList)

## Architecture Overview

### Core Components

1. **Models** (`model` package)
   - `SensorRoom`: Represents a physical room in the campus
   - `Sensor`: Represents IoT sensors deployed in rooms
   - `SensorReading`: Historical reading data from sensors

2. **Repository** (`repository` package)
   - `DataStore`: Centralized in-memory data repository using thread-safe ConcurrentHashMap

3. **Resources** (`resource` package)
   - `DiscoveryResource`: API metadata and navigation endpoint
   - `SensorRoomResource`: Room management endpoints
   - `SensorResource`: Sensor management and filtering
   - `SensorReadingResource`: Sub-resource for sensor reading history

4. **Exception Handling** (`exception` package)
   - `CustomExceptions`: Container class with custom exception types
   - `ErrorResponse`: Standardized error response format

5. **Exception Mappers** (`mapper` package)
   - `RoomNotEmptyExceptionMapper`: HTTP 409 Conflict
   - `LinkedResourceNotFoundExceptionMapper`: HTTP 422 Unprocessable Entity
   - `SensorUnavailableExceptionMapper`: HTTP 403 Forbidden
   - `GenericExceptionMapper`: HTTP 500 Internal Server Error

6. **Filters** (`filter` package)
   - `LoggingFilter`: Request/Response logging using Java logging

## JAX-RS Resource Lifecycle

In JAX-RS, resource classes are typically **instantiated once per request** by default. This means a fresh instance of each resource class (e.g., `SensorRoomResource`, `SensorResource`) is created by the JAX-RS runtime for every incoming HTTP request. Once the request is processed and the response is sent, the instance is discarded and eligible for garbage collection.

### Impact on Data Management:
- Since resource instances are short-lived, any data stored directly as instance fields would be lost after the request completes.
- To persist data across requests, we use the **Singleton pattern** via `DataStore.getInstance()`. This ensures all resource instances share the same data store.
- The `DataStore` class uses **`ConcurrentHashMap`** for thread-safe access, preventing race conditions when multiple requests modify data concurrently.
- This combination of per-request resources and a singleton data store provides both isolation (each request gets a fresh resource) and persistence (data survives across requests).

## HATEOAS and Hypermedia

This API implements HATEOAS (Hypermedia As The Engine Of Application State) principles by providing a discovery endpoint at `GET /api/v1` that returns:
- API versioning information
- Administrative contact details
- A map of primary resource collections with their URI paths

**Benefits of Hypermedia over Static Documentation:**
- **Self-Discoverable**: Clients can navigate the entire API starting from a single root URL, without prior knowledge of URI patterns. This makes integration easier for new developers.
- **Decoupling**: Clients follow links rather than hardcoding URI patterns, so server-side URI changes don't break client implementations.
- **API Evolution**: APIs can evolve gracefully — new resources can be added to the discovery endpoint without breaking existing clients who simply ignore unknown links.
- **Reduced Documentation Dependency**: URIs are embedded in responses, reducing reliance on external documentation that may become outdated.
- **Dynamic Navigation**: Clients can discover which operations are available at runtime based on the current state of the resource.

## API Endpoints

### Discovery & Health
```
GET  /api/v1              - API metadata and resource discovery
GET  /api/v1/health       - Health check endpoint
```

### Room Management
```
GET    /api/v1/rooms              - List all rooms
POST   /api/v1/rooms              - Create a new room
GET    /api/v1/rooms/{roomId}     - Get room details
DELETE /api/v1/rooms/{roomId}     - Delete a room (cannot delete if sensors exist)
```

### Sensor Management
```
GET    /api/v1/sensors                          - List all sensors (optional type filtering)
GET    /api/v1/sensors?type=Temperature         - Filter sensors by type
POST   /api/v1/sensors                          - Register a new sensor
GET    /api/v1/sensors/{sensorId}               - Get sensor details
PUT    /api/v1/sensors/{sensorId}               - Update sensor properties
DELETE /api/v1/sensors/{sensorId}               - Delete a sensor
```

### Sensor Readings (Sub-Resource)
```
GET    /api/v1/sensors/{sensorId}/readings      - Get reading history for a sensor
POST   /api/v1/sensors/{sensorId}/readings      - Add a new reading to a sensor
```

## Setup & Installation

### Prerequisites
- Java Development Kit (JDK) 11 or higher
- Apache Maven 3.6 or higher
- Apache Tomcat 10.1 or higher (with Jakarta Servlet 6.0 support)
- Git

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd CSA
   ```

2. **Build the project:**
   ```bash
   mvn clean package
   ```
   
   This will:
   - Compile the source code
   - Run tests (if any)
   - Create a WAR file at `target/smart-campus-api.war`

3. **Deploy to Tomcat:**
   - Copy the generated `target/smart-campus-api.war` file to the `webapps/` directory of your Tomcat installation.
   - Start (or restart) Tomcat.
   - Tomcat will automatically deploy the WAR file.

4. **Verify the server is running:**
   ```bash
   curl http://localhost:8080/smart-campus-api/api/v1
   ```

The API will be accessible at `http://localhost:8080/smart-campus-api/api/v1`

## Sample cURL Commands

### 1. Discover API Resources
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1 -H "Accept: application/json"
```

**Expected Response:**
```json
{
  "api_version": "1.0.0",
  "title": "Smart Campus Sensor & Room Management API",
  "description": "RESTful API for managing rooms and sensors in a smart campus environment",
  "contact": {
    "name": "API Support",
    "email": "support@smartcampus.edu"
  },
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors",
    "health": "/api/v1/health"
  },
  "timestamp": 1672531200000
}
```

### 2. Get All Rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms -H "Accept: application/json"
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"id\": \"AUD-405\", \"name\": \"Auditorium 405\", \"capacity\": 300, \"sensorIds\": []}"
```

### 4. Get All Sensors (with optional type filter)
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors -H "Accept: application/json"
```

### 5. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=Temperature" -H "Accept: application/json"
```

### 6. Create a New Sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"id\": \"TEMP-003\", \"type\": \"Temperature\", \"status\": \"ACTIVE\", \"currentValue\": 23.5, \"roomId\": \"LIB-301\"}"
```

### 7. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"value\": 24.1}"
```

### 8. Get Sensor Reading History
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings -H "Accept: application/json"
```

### 9. Delete a Room (will fail if sensors exist — returns 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LEC-101 -H "Accept: application/json"
```

### 10. Health Check
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/health -H "Accept: application/json"
```

## Error Handling

The API implements comprehensive error handling with standardized JSON responses:

### Error Response Format
```json
{
  "status": 409,
  "message": "Cannot delete room LIB-301 because it contains 2 active sensor(s)",
  "errorCode": "ROOM_NOT_EMPTY",
  "timestamp": "1672531200000"
}
```

### HTTP Status Codes

| Status | Scenario | Example |
|--------|----------|---------|
| 200 | Successful GET, PUT | Room retrieved successfully |
| 201 | Resource created | New room created |
| 204 | Successful DELETE | Room deleted |
| 400 | Bad request | Missing required fields |
| 403 | Forbidden | Sensor in MAINTENANCE state |
| 404 | Not found | Room ID doesn't exist |
| 409 | Conflict | Room has active sensors |
| 415 | Unsupported Media Type | Non-JSON content type sent |
| 422 | Unprocessable entity | Referenced room doesn't exist |
| 500 | Server error | Unexpected exception |

## Conceptual Report

### Part 1: JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton?

By default, JAX-RS resource classes follow a **per-request lifecycle**. This means the JAX-RS runtime (Jersey, in our case) creates a **new instance** of the resource class for every incoming HTTP request. Once the request has been processed and the response sent, the instance is discarded and becomes eligible for garbage collection.

This architectural decision has significant implications for data management:
- **Instance fields are request-scoped**: Any data stored in instance variables of a resource class is lost after the request completes.
- **Thread safety of the resource itself**: Since each request gets its own instance, there are no race conditions on the resource's own fields.
- **Shared data requires external management**: To persist data across requests, we use the Singleton pattern via `DataStore.getInstance()`, which returns the same `DataStore` instance to all resource instances.
- **Synchronization of shared data**: The `DataStore` uses `ConcurrentHashMap` (a thread-safe implementation of `Map`) to handle concurrent read/write operations from multiple request threads. This prevents data corruption, lost updates, and race conditions without requiring explicit synchronized blocks.

### Part 1: HATEOAS and Hypermedia

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)?

HATEOAS (Hypermedia As The Engine Of Application State) is considered the highest maturity level of REST APIs (Level 3 in the Richardson Maturity Model). It means that API responses contain not just data, but also links to related actions and resources.

**Benefits over static documentation:**
1. **Self-Discovery**: Client developers can explore the entire API by following links from the root endpoint, without needing to read extensive documentation first.
2. **Decoupling**: Clients that follow hyperlinks are resilient to URI changes — if the server restructures its URLs, the links in responses automatically reflect the new paths.
3. **Reduced Errors**: Developers don't need to manually construct URIs, reducing the chance of typos or incorrect path construction.
4. **Evolvability**: New resources and capabilities can be added to the API without updating client code — clients simply discover new links in responses.
5. **State-Driven Navigation**: Links can be conditionally included based on the current state of a resource, guiding clients toward valid operations.

### Part 2: Returning IDs vs Full Objects

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects?

- **Returning only IDs**: Reduces network bandwidth usage significantly, especially with large collections. However, it forces clients to make additional HTTP requests (one per ID) to fetch the full details, increasing overall latency and server load. This N+1 query pattern can be very inefficient.
- **Returning full objects**: Increases the payload size per response but eliminates the need for follow-up requests. This reduces total round-trip time and simplifies client-side logic. This approach is generally preferred for small-to-medium collections, which is why this API returns full room objects.

### Part 2: DELETE Idempotency

**Question:** Is the DELETE operation idempotent in your implementation?

Yes, the DELETE operation is **idempotent** in this implementation. Idempotency means that making the same request multiple times produces the same result as making it once.

- **First DELETE request**: The room is found, validated (no sensors attached), and deleted. Returns `204 No Content`.
- **Second DELETE request (same room ID)**: The room no longer exists in the data store. Returns `404 Not Found`.
- **Third DELETE request (same room ID)**: Same as second — returns `404 Not Found`.

The server state after the second and subsequent DELETE requests is identical to the state after the first successful deletion (the room remains deleted). While the HTTP status code changes from 204 to 404, the server-side effect (room is gone) is the same, satisfying the definition of idempotency. This is safe for clients who might retry failed requests due to network issues.

### Part 3: @Consumes Mismatch

**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`.

When a POST method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS enforces strict content type matching. If a client sends a request with a `Content-Type` header of `text/plain` or `application/xml`:

1. The JAX-RS runtime intercepts the request **before** the method is invoked.
2. It checks the `Content-Type` header against the `@Consumes` annotation.
3. Finding no match, it automatically returns an **HTTP 415 Unsupported Media Type** response.
4. The resource method body is **never executed**, preventing potential deserialization errors.

This mechanism acts as a gatekeeper, ensuring that only properly formatted JSON data reaches the business logic, improving both security and reliability.

### Part 3: @QueryParam vs Path Parameter for Filtering

**Question:** Contrast `@QueryParam` with an alternative design where the type is part of the URL path. Why is the query parameter approach generally considered superior for filtering?

- **`@QueryParam("type")` approach** (`/api/v1/sensors?type=CO2`):
  - Query parameters are **optional** by nature — omitting the parameter returns all sensors.
  - Multiple filters can be **combined easily** (e.g., `?type=CO2&status=ACTIVE`).
  - The base URI `/api/v1/sensors` clearly identifies the **resource collection**.
  - This follows the REST convention that query parameters modify **how** a collection is retrieved, not **what** the resource is.

- **Path-based approach** (`/api/v1/sensors/type/CO2`):
  - Makes the filter look like a **sub-resource**, which is semantically misleading — "type/CO2" is not a resource.
  - Combining multiple filters creates awkward paths (e.g., `/sensors/type/CO2/status/ACTIVE`).
  - The base collection URI is obscured.
  - Omitting the filter requires a completely different path.

The query parameter approach is superior because it maintains clear resource identification while providing flexible, combinable filtering.

### Part 4: Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern.

The Sub-Resource Locator pattern (used in `SensorResource.getReadingsSubResource()`) delegates request handling to a separate class rather than defining all nested paths in one controller.

**Benefits:**
1. **Separation of Concerns**: `SensorResource` handles sensor CRUD, while `SensorReadingResource` handles reading operations. Each class has a single responsibility.
2. **Reduced Complexity**: Without sub-resources, a single controller managing `/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, and potentially `/sensors/{id}/readings/{rid}` would grow unmanageably large.
3. **Reusability**: The `SensorReadingResource` class could potentially be reused in different contexts or by different parent resources.
4. **Independent Testing**: Each sub-resource class can be unit-tested independently without needing the parent resource context.
5. **Contextual Processing**: The parent resource can perform validation (e.g., checking if the sensor exists) before delegating, ensuring the sub-resource only handles valid contexts.

### Part 5: HTTP 422 vs 404

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

- **HTTP 404 Not Found**: Indicates that the **request target** (the URL) does not correspond to an existing resource. It refers to the endpoint itself.
- **HTTP 422 Unprocessable Entity**: Indicates that the server understood the request and the JSON payload is well-formed, but the **content** of the payload is semantically invalid.

When a client POSTs a sensor with a `roomId` that doesn't exist, the request URL (`/api/v1/sensors`) is valid, and the JSON body is syntactically correct. The problem is that a **reference within the payload** points to a non-existent resource. Using 404 would misleadingly suggest that the `/api/v1/sensors` endpoint doesn't exist. Using 422 correctly communicates: "Your request was syntactically valid, but the data you provided cannot be processed because it references a non-existent room."

### Part 5: Security of Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers.

Exposing raw stack traces to API consumers is a significant security vulnerability known as **information disclosure**:

1. **Framework Identification**: Stack traces reveal the exact framework and version being used (e.g., "Jersey 3.1.1", "Tomcat 10.1"), allowing attackers to search for known CVEs (Common Vulnerabilities and Exposures) specific to those versions.
2. **Internal Architecture Exposure**: Package names like `com.smartcampus.repository.DataStore` reveal the internal code structure, making it easier for attackers to understand the application architecture and identify potential attack vectors.
3. **Database/Configuration Details**: Stack traces from database errors might expose connection strings, table names, or SQL queries, providing valuable information for SQL injection attacks.
4. **File Path Disclosure**: Stack traces often include absolute file paths on the server, revealing the operating system, deployment structure, and potentially sensitive directory names.
5. **Third-Party Library Exposure**: All dependencies and their versions are visible, expanding the attack surface to include vulnerabilities in any of those libraries.

This is why our `GenericExceptionMapper` returns a generic error message to the client while logging the actual exception details server-side for debugging.

### Part 5: Filters for Cross-Cutting Concerns

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

1. **DRY Principle**: A single filter class handles logging for **all** endpoints, eliminating code duplication across dozens of resource methods.
2. **Consistency**: Filters guarantee that every request and response is logged in a uniform format. Manual logging risks inconsistent messages or missed endpoints.
3. **Separation of Concerns**: Business logic in resource methods remains clean and focused. Logging, authentication, CORS, and other cross-cutting concerns are handled orthogonally.
4. **Maintainability**: Changing the log format or adding new logging fields requires modifying only one class, not every resource method.
5. **Automatic Coverage**: New endpoints are automatically covered by the filter without any additional code. With manual logging, developers might forget to add log statements to new methods.
6. **Enable/Disable Globally**: Filters can be registered or unregistered in the `Application` class, making it trivial to enable or disable logging across the entire API.

---

**Version**: 1.0.0  
**Last Updated**: April 2026  
**API Contact**: support@smartcampus.edu
