# Smart Campus API — Conceptual Report

**Module**: 5COSC022W Client-Server Architectures  
**Student**: [YOUR NAME]  
**Student ID**: [YOUR ID]

---

## Part 1: Service Architecture & Setup

### Question 1.1: JAX-RS Resource Lifecycle

**Question**: *Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures to prevent data loss or race conditions.*

In JAX-RS, resource classes follow a **per-request lifecycle** by default. This means the JAX-RS runtime (Jersey, in this implementation) creates a **new instance** of each resource class — such as `SensorRoomResource` or `SensorResource` — for every incoming HTTP request. Once the request is processed and the response is sent back to the client, the resource instance is discarded and becomes eligible for garbage collection.

This design has important implications for how data is managed:

1. **Instance fields are request-scoped**: Any data stored in instance variables of a resource class is lost when the request completes. If we were to store room or sensor data in a resource's instance field, it would disappear after every request.

2. **Thread safety of the resource itself**: Since each request gets its own instance, there are no race conditions on the resource's own fields — multiple requests never share the same resource object.

3. **Shared data requires external management**: To persist data across requests, this implementation uses the **Singleton pattern** via `DataStore.getInstance()`. The `DataStore` class has a private constructor and a `synchronized` static method that ensures only one instance exists application-wide. Every resource instance — regardless of which request it serves — accesses the same `DataStore` singleton.

4. **Synchronization of shared data**: The `DataStore` uses `ConcurrentHashMap` rather than `HashMap` for its internal collections. `ConcurrentHashMap` is a thread-safe implementation of `Map` from the `java.util.concurrent` package. It allows multiple threads to read and write simultaneously without corruption by using fine-grained locking (lock striping). This eliminates the need for explicit `synchronized` blocks in the resource classes while still preventing:
   - **Lost updates**: Two threads modifying the same sensor concurrently won't overwrite each other's changes.
   - **Dirty reads**: A thread won't read partially-written data.
   - **Race conditions**: Operations like `put()` and `remove()` are atomic.

This combination of per-request resources with a thread-safe singleton data store provides both **isolation** (each request gets a clean resource) and **persistence** (data survives across requests) — the best of both approaches.

---

### Question 1.2: HATEOAS and Hypermedia

**Question**: *Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?*

HATEOAS (Hypermedia As The Engine Of Application State) is considered the highest maturity level of REST APIs — **Level 3** in the Richardson Maturity Model. It means that API responses contain not just data, but also **navigational links** to related actions and resources, enabling clients to discover what they can do next without prior knowledge.

In this implementation, the discovery endpoint at `GET /api/v1` returns a `resources` map containing URIs for all primary collections:
```json
{
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors",
    "health": "/api/v1/health"
  }
}
```

**Benefits over static documentation:**

1. **Self-Discovery**: A client developer only needs to know the single root URL (`/api/v1`). From there, the response tells them where rooms and sensors live. They don't need to read a 50-page API specification before making their first request.

2. **Decoupling from URI structure**: When clients follow links from responses rather than hardcoding URIs (e.g., `"/api/v1/rooms"`), the server can change its URL structure without breaking clients. For instance, if we later move rooms to `/api/v2/campus/rooms`, clients that follow links from the discovery endpoint would automatically get the new path.

3. **Evolvability**: New resources can be added to the API by simply adding new entries to the discovery response. Existing clients that don't recognise the new link simply ignore it — no breaking changes, no version bumps required.

4. **Reduced documentation dependency**: Traditional APIs require developers to bookmark external documentation that frequently becomes outdated. With HATEOAS, the API itself serves as living documentation — the URIs and available actions are always accurate because they come from the running server.

5. **State-driven navigation**: In more advanced implementations, links can appear or disappear based on the current state of a resource. For example, a room with sensors might not include a "delete" link, while an empty room would — guiding clients toward valid operations only.

---

## Part 2: Room Management

### Question 2.1: Returning IDs vs Full Objects

**Question**: *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.*

There are significant trade-offs between returning only room IDs versus full room objects:

**Returning only IDs** (e.g., `["LIB-301", "LEC-101", "LAB-205"]`):
- **Advantage — Bandwidth**: The response payload is much smaller, sometimes by an order of magnitude. For 1,000 rooms, returning just IDs might be 10KB versus 500KB for full objects.
- **Disadvantage — N+1 Problem**: The client must make a separate HTTP request for each room to get its details. For 1,000 rooms, this means 1,001 total requests (1 for the list + 1,000 for details). Each request incurs network latency (TCP handshake, TLS negotiation, HTTP overhead), making this approach extremely inefficient.
- **Disadvantage — Client complexity**: The client must implement logic to iterate through IDs, make individual requests, handle failures, and assemble the complete data set.

**Returning full objects** (our implementation):
- **Advantage — Fewer requests**: A single GET request returns all the data the client needs. This reduces total round-trip time from potentially thousands of requests to just one.
- **Advantage — Simplicity**: The client receives a complete, ready-to-use collection with no additional processing required.
- **Disadvantage — Bandwidth**: Larger response payload. For very large collections with thousands of items, this could become problematic.
- **Mitigation**: For large datasets, pagination can be implemented (e.g., `?page=1&size=20`) to balance between completeness and payload size.

This API returns **full room objects** because the dataset is small enough that bandwidth is not a concern, and it significantly improves the developer experience by eliminating the need for follow-up requests.

---

### Question 2.2: DELETE Idempotency

**Question**: *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.*

Yes, the DELETE operation is **idempotent** in this implementation. Idempotency means that performing the same operation multiple times produces the same **server-side state** as performing it once.

Here is what happens across multiple identical DELETE requests for the same room:

| Request | Server State Before | Action | Response | Server State After |
|---------|-------------------|--------|----------|-------------------|
| 1st DELETE `/rooms/AUD-405` | Room exists | Room is removed from DataStore | **204 No Content** | Room gone |
| 2nd DELETE `/rooms/AUD-405` | Room does NOT exist | Nothing to delete | **404 Not Found** | Room still gone |
| 3rd DELETE `/rooms/AUD-405` | Room does NOT exist | Nothing to delete | **404 Not Found** | Room still gone |

The key observation is that while the **HTTP status code changes** from 204 to 404 after the first request, the **server-side state remains identical** after the first DELETE and all subsequent ones — the room `AUD-405` is absent from the system. No additional data is modified, no side effects occur, and no resources are created.

This is important because:
- **Network reliability**: If a client sends a DELETE request and doesn't receive a response due to a network timeout, it can safely retry the request without worrying about unintended consequences.
- **Distributed systems**: In microservice architectures, messages may be delivered more than once. Idempotent DELETE operations ensure that duplicate messages don't cause data corruption.
- **HTTP specification compliance**: RFC 7231 explicitly states that DELETE should be idempotent, and our implementation adheres to this standard.

---

## Part 3: Sensor Operations & Linking

### Question 3.1: @Consumes Annotation Mismatch

**Question**: *We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?*

When a POST method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime enforces **strict content type matching** before the method is ever invoked. Here is the exact sequence of events when a client sends a non-JSON request:

1. **Client sends request**: The client sends a POST request with a `Content-Type` header of `text/plain` or `application/xml`.

2. **JAX-RS content negotiation**: The runtime examines the incoming `Content-Type` header and compares it against the `@Consumes` annotation on all candidate resource methods. Since the only matching method requires `application/json`, there is no compatible method for `text/plain`.

3. **Automatic rejection**: The JAX-RS runtime returns an **HTTP 415 Unsupported Media Type** response. This happens at the framework level — the resource method body is **never executed**.

4. **No deserialization attempted**: Because the runtime rejects the request before reaching the method, no JSON-B (or Jackson) deserialization is attempted. This means there is no risk of:
   - `ClassCastException` from attempting to parse plain text as a JSON object
   - `JsonParsingException` from malformed data
   - Security vulnerabilities from injecting unexpected data formats

This mechanism acts as a **gatekeeper**, ensuring that only properly formatted JSON data reaches the business logic layer. It is one of the benefits of declarative annotations — security and validation are handled automatically by the framework rather than requiring manual checks in every method.

The same principle applies in reverse with `@Produces(MediaType.APPLICATION_JSON)`: if a client sends an `Accept: text/xml` header, the runtime returns **HTTP 406 Not Acceptable** because the server cannot produce the requested format.

---

### Question 3.2: @QueryParam vs Path Parameter for Filtering

**Question**: *You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?*

The two approaches differ fundamentally in how they model the relationship between the filter and the resource:

**Query Parameter approach** (`/api/v1/sensors?type=CO2`) — **our implementation**:

1. **Filters are optional**: Omitting the `?type=` parameter returns all sensors. The base URI `/api/v1/sensors` remains a valid, meaningful endpoint representing the entire collection.

2. **Combinable filters**: Multiple filters can be easily chained: `?type=CO2&status=ACTIVE&roomId=LIB-301`. This allows powerful, flexible querying without any URL structure changes.

3. **Clear resource identity**: The URI `/api/v1/sensors` unambiguously identifies the "sensors collection" resource. The query string modifies *how* that collection is retrieved, not *what* the resource is.

4. **RESTful semantics**: In REST, the URI identifies a **resource**. Query parameters represent **non-hierarchical** modifiers like filtering, sorting, and pagination. This matches the HTTP specification's intent.

5. **Caching**: Intermediary caches can cache `/api/v1/sensors` separately from `/api/v1/sensors?type=CO2`, which is standard HTTP caching behaviour.

**Path-based approach** (`/api/v1/sensors/type/CO2`):

1. **Misleading hierarchy**: This makes "type" and "CO2" look like sub-resources of sensors, which is semantically incorrect — "CO2" is not a resource, it's a filter criterion.

2. **Rigid structure**: Combining multiple filters creates increasingly awkward and deeply nested paths: `/api/v1/sensors/type/CO2/status/ACTIVE/room/LIB-301`. The order of path segments also becomes ambiguous — is it `/type/CO2/status/ACTIVE` or `/status/ACTIVE/type/CO2`?

3. **Not optional**: Every path segment is required for the URL to resolve. Making a filter optional would require defining entirely separate endpoint methods.

4. **Breaks resource hierarchy**: The sensors collection URI is obscured — `/api/v1/sensors/type/CO2` doesn't clearly convey that it's filtering the sensors collection.

The query parameter approach is the industry standard for filtering and is recommended by REST API design guidelines because it maintains clean resource identification while providing maximum flexibility.

---

## Part 4: Deep Nesting with Sub-Resources

### Question 4.1: Sub-Resource Locator Pattern

**Question**: *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?*

The Sub-Resource Locator pattern, used in `SensorResource.getReadingsSubResource()`, is a JAX-RS mechanism where a resource method **returns an object** instead of a `Response`. The returned object becomes a new resource that handles the remaining path segments.

In this implementation:
```java
@Path("{sensorId}/readings")
public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
    // Validate sensor exists, then delegate
    return new SensorReadingResource(sensorId);
}
```

**Architectural Benefits:**

1. **Separation of Concerns (Single Responsibility Principle)**: `SensorResource` handles sensor CRUD operations (`/sensors`, `/sensors/{id}`), while `SensorReadingResource` handles reading operations (`/sensors/{id}/readings`). Each class has exactly one responsibility, making the code easier to understand and modify.

2. **Reduced class complexity**: Without sub-resources, `SensorResource` would contain methods for:
   - `GET /sensors` (list all)
   - `POST /sensors` (create)
   - `GET /sensors/{id}` (get one)
   - `PUT /sensors/{id}` (update)
   - `DELETE /sensors/{id}` (delete)
   - `GET /sensors/{id}/readings` (list readings)
   - `POST /sensors/{id}/readings` (add reading)
   - Potentially `GET /sensors/{id}/readings/{rid}` (get specific reading)
   - And more nested paths...

   This "God class" anti-pattern leads to files with hundreds of lines, mixed concerns, and difficult-to-trace bugs. Sub-resources keep each class focused and manageable.

3. **Contextual validation**: The parent resource method (`getReadingsSubResource`) can perform validation before delegation. In our case, it verifies that the sensor exists before returning the `SensorReadingResource`. This prevents the sub-resource from having to repeat this validation in every one of its methods.

4. **Independent testing**: `SensorReadingResource` can be unit-tested in isolation by instantiating it with a known sensor ID. There is no need to set up the entire `SensorResource` context — the sub-resource is a self-contained unit.

5. **Reusability**: The sub-resource class could be reused by different parent resources. If a future `RoomResource` needed to provide readings for all sensors in a room, it could delegate to the same `SensorReadingResource` class.

6. **Scalability**: As the API grows, new sub-resources (e.g., `/sensors/{id}/alerts`, `/sensors/{id}/maintenance-log`) can be added as separate classes without modifying the existing `SensorResource` code — adhering to the Open/Closed Principle.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging

### Question 5.1: HTTP 422 vs 404

**Question**: *Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?*

The distinction between 422 and 404 lies in **what is missing** and **where the problem originates**:

**HTTP 404 Not Found** means the **request target** — the URL itself — does not correspond to an existing resource. It answers the question: "Does this endpoint exist?" For example:
- `GET /api/v1/rooms/NONEXISTENT` → 404 is correct because the URL path `/rooms/NONEXISTENT` points to a resource that doesn't exist.

**HTTP 422 Unprocessable Entity** means the server understood the request, the URL is valid, the JSON syntax is correct, but the **content** of the request body is semantically invalid. It answers the question: "Is the data valid in the context of this operation?"

When a client sends:
```json
POST /api/v1/sensors
{
  "id": "TEMP-003",
  "roomId": "DOES-NOT-EXIST"
}
```

- The URL `/api/v1/sensors` **does exist** — it's the sensors collection endpoint.
- The JSON body **is syntactically valid** — it parses correctly.
- The problem is that `roomId: "DOES-NOT-EXIST"` is a **reference to a resource that doesn't exist**.

Using 404 here would misleadingly suggest that the `/api/v1/sensors` endpoint itself doesn't exist, confusing the client developer. Using 422 correctly communicates: "Your request was well-formed and sent to the right place, but the data you provided cannot be processed because it contains an invalid reference."

This distinction is important for client-side error handling:
- **404**: The client should check its URL construction — perhaps there's a typo in the endpoint path.
- **422**: The URL is fine; the client should check the data it's sending — perhaps the referenced room needs to be created first.

HTTP 422 was originally defined in RFC 4918 (WebDAV) but has been widely adopted for REST API validation errors because it fills a semantic gap that 400 (Bad Request — malformed syntax) doesn't fully cover.

---

### Question 5.2: Security Risks of Exposing Stack Traces

**Question**: *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?*

Exposing raw stack traces is classified as an **information disclosure vulnerability** (CWE-209: Generation of Error Message Containing Sensitive Information) and is listed in the OWASP Top 10 as a common security misconfiguration.

**Specific information an attacker can extract:**

1. **Framework and version identification**: A stack trace like `org.glassfish.jersey.server.ServerRuntime$Responder` immediately reveals that Jersey is the JAX-RS implementation. The attacker can then search for known CVEs (Common Vulnerabilities and Exposures) specific to that version, such as deserialization vulnerabilities or authentication bypasses.

2. **Internal package structure**: Stack traces expose package names like `com.smartcampus.repository.DataStore` and `com.smartcampus.resource.SensorResource`, revealing the internal architecture, naming conventions, and code organization. This helps an attacker understand how the application is structured and where to probe for weaknesses.

3. **Database and connection details**: Stack traces from database-related exceptions might reveal:
   - JDBC connection strings (including hostnames, ports, and database names)
   - SQL queries (enabling SQL injection attacks)
   - Table and column names (facilitating targeted attacks)

4. **File system paths**: Traces include absolute file paths like `C:\Users\deploy\smartcampus\src\main\java\...`, revealing the operating system (Windows vs Linux), deployment directory structure, and potentially sensitive usernames.

5. **Third-party library inventory**: All dependency classes appear in the trace (e.g., `com.fasterxml.jackson`, `org.eclipse.yasson`), giving the attacker a complete list of libraries to check for known vulnerabilities.

6. **Method-level logic exposure**: Stack frames reveal method names and call sequences, helping the attacker understand the internal processing pipeline and identify potential business logic flaws.

**Our mitigation**: The `GenericExceptionMapper` in this implementation catches all `Throwable` exceptions and returns a **generic, opaque message**: `"An unexpected error occurred. Please contact support."` The actual exception details are logged server-side using `java.util.logging.Logger` for debugging purposes but are **never transmitted to the client**. This ensures operational visibility for developers while maintaining security against external threats.

---

### Question 5.3: JAX-RS Filters for Cross-Cutting Concerns

**Question**: *Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?*

Cross-cutting concerns are functionalities that span multiple components of an application — logging, authentication, CORS, rate limiting — and are orthogonal to the core business logic. JAX-RS filters provide a **declarative, centralized mechanism** to handle these concerns.

**Advantages of the filter approach:**

1. **DRY Principle (Don't Repeat Yourself)**: Our single `LoggingFilter` class handles logging for **every endpoint** in the entire API — rooms, sensors, readings, discovery, health — all with one class. With manual logging, every method in every resource would need identical `LOGGER.info()` statements at the beginning and end, leading to massive code duplication.

2. **Guaranteed consistency**: The filter ensures that every request and response is logged in an **identical format**:
   ```
   Incoming Request: [GET] http://localhost:8080/smart-campus-api/api/v1/rooms
   Response Status: 200 for [GET] http://localhost:8080/smart-campus-api/api/v1/rooms
   ```
   With manual logging, different developers might use different formats, miss certain log fields, or forget to log certain methods altogether.

3. **Automatic coverage for new endpoints**: When a new resource class or method is added to the API, the filter automatically logs its requests and responses without any additional code. With manual logging, the developer must remember to add log statements to every new method — a common source of gaps.

4. **Separation of concerns**: Resource methods remain clean and focused on business logic. Mixing logging, timing, authentication, and data processing in the same method creates "spaghetti code" that is hard to read and maintain.

5. **Easy to enable/disable globally**: Filters are registered in the `Application` class (or via `@Provider` annotation). Removing a single line in `SmartCampusApplication.getClasses()` disables logging across the entire API. With manual statements, you'd need to edit dozens of methods across multiple files.

6. **Dual filter capability**: Our `LoggingFilter` implements both `ContainerRequestFilter` (pre-processing) and `ContainerResponseFilter` (post-processing), allowing it to capture the complete request-response lifecycle in one class. This is not easily achievable with manual logging, as you'd need try-finally blocks in every method.

7. **Composability**: Multiple filters can be chained together (e.g., logging → authentication → CORS → rate limiting) with defined execution order using `@Priority` annotations. Each filter remains a small, focused, testable unit.

---

*End of Report*
